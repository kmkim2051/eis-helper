# 운영 아키텍처 (AWS)

> 이 문서는 물리적 배포 구조(인프라)를 정의한다. 비즈니스 시스템 설계는 [service-design.md](service-design.md) 참고.
> 2026-07-10 결정, 외부 피드백 반영 보완.

## 전제

- 프런트와 백엔드는 **같은 도메인**으로 노출하고 `/api/**`만 백엔드로 라우팅한다 (CORS 미사용).
- 프론트엔드는 항상 상대 경로 `/api`로 API를 호출한다. 로컬 개발의 vite proxy와 운영의 reverse proxy가 같은 구조로 대응된다.
- **Spring Boot API는 `/api` prefix를 유지한다** (컨트롤러가 `/api/**`를 직접 매핑). Nginx rewrite가 불필요하고, 이후 ALB/CloudFront 이전 시 path 라우팅이 단순해진다.

## 1단계 — MVP 공개용 (현재 목표)

구성요소를 최소화한다. guidelines의 "초기에는 단순 배포로 충분" 원칙을 따른다.

```
Route53
  ↓
Elastic IP
  ↓
EC2 1대 (t4g.small)
  ├─ Nginx (80/443 진입점, TLS 종료)
  │    ├─ /       → Vite 빌드 정적 파일 서빙 (/var/www/app)
  │    └─ /api/** → localhost:8080 (Spring Boot)
  │
  └─ Docker Compose
       ├─ Spring Boot API (localhost:8080 bind)
       └─ MariaDB (Docker network 내부만 노출, data volume 분리)
```

- **HTTPS**: EC2 단독 구성에서는 ACM 인증서를 붙일 수 없다(ACM은 ALB/CloudFront 등 관리형 엔드포인트 전용). **Let's Encrypt + certbot**을 사용한다.
- **1단계에서는 ALB를 사용하지 않는다.** Nginx가 TLS 종료, 정적 파일 서빙, `/api` reverse proxy, gzip, proxy timeout을 모두 담당한다.
- **무중단 배포를 목표로 하지 않는다.** 배포 시 수초~수십 초의 중단을 허용한다. (무중단이 필요해지는 시점이 2단계 이전 신호 중 하나)

### Nginx 라우팅 원칙 (SPA fallback 충돌 주의)

SPA는 `/problems/1` 같은 클라이언트 라우트를 가지므로 정적 파일에 없는 경로는 index.html로 fallback해야 하고, `/api/**`는 반드시 Spring으로 가야 한다. location 우선순위를 다음 구조로 고정한다.

```nginx
location /api/ {
    proxy_pass http://localhost:8080;
}
location / {
    try_files $uri $uri/ /index.html;
}
```

이 순서가 어긋나면 `/api` 요청이 index.html로 떨어지거나 프런트 라우트가 404가 된다.

## 2단계 — 트래픽/가용성 요구가 생기면

같은 도메인 + `/api` 라우팅 정책을 유지하기 위해 **CloudFront를 단일 진입점**으로 둔다.
(Route53이 같은 도메인을 S3와 ALB로 동시에 보낼 수는 없으므로, path 분기는 CloudFront origin 라우팅으로 한다. 도메인을 `api.example.com`으로 나누면 CORS가 부활하므로 채택하지 않는다.)

```
Route53
  ↓
CloudFront (ACM 인증서 연결, 단일 진입점)
  ├─ / (default) → S3 Origin (Vite 빌드 정적 파일)
  └─ /api/*      → ALB Origin
                      ↓
                   EC2 API 인스턴스 2대+ (Auto Scaling Group)
                      ↓
                   RDS MariaDB
                      ↓
                   ElastiCache Redis (필요 시)
```

CloudFront 구성 주의사항:

- `/api/*`는 캐싱 비활성화(또는 최소화)
- Authorization/Cookie 등 필요한 헤더를 origin으로 전달하는 origin request policy 설정
- 필요한 HTTP method(POST 포함) 허용

클라이언트는 계속 상대 경로 `/api`만 호출하므로 **1단계 → 2단계 이전에 프런트/백엔드 코드 변경이 없다.** 이것이 이 설계의 핵심이다.

## 구성요소별 결정 이유

- **front 전용 VM 없음** — Vite 빌드 산출물은 정적 파일이라 서버 프로세스가 불필요. Nginx 서빙(1단계) 또는 S3+CloudFront(2단계)
- **reverse proxy와 LB의 역할 분담** — 1단계에서는 Nginx가 reverse proxy와 TLS 종료를 담당한다. 2단계에서는 ALB가 외부 진입/TLS/헬스체크/로드밸런싱을 담당하고, 이때 인스턴스 내부 Nginx는 유지할 수도 있으나 대부분 `ALB → Spring Boot` 직결로 충분하므로 중복 구성을 피한다
- **DB는 관리형 지향** — 자체 운영은 백업/패치/장애 복구 부담. 초기 Compose 컨테이너 → 데이터 보존 중요도가 올라가면 RDS(single-AZ, t4g.micro)로 이전
- **Redis 보류** — 세션 없음(익명), 캐시 압력 없음. Phase 2 LLM 이후 rate limit, 동일 답안 hash 기반 채점 캐시, 비동기 채점 job 상태 등 실사용처가 생기면 ElastiCache로 도입 (guidelines "과도한 기능 확장 회피")

## 필수 운영 원칙

### 네트워크/보안그룹

- Nginx만 80/443으로 외부 요청을 받는다.
- Spring Boot 8080 포트는 외부에 직접 노출하지 않는다 (localhost bind).
- MariaDB 3306 포트는 외부에 직접 노출하지 않는다 (Docker network 내부만).
- SSH(22)는 내 IP로 제한하거나 SSM Session Manager를 사용한다.

### 시크릿 관리 (SSM Parameter Store 주입 절차)

시크릿(DB 비밀번호, LLM API 키)은 SSM Parameter Store **SecureString**(KMS 암호화)으로 관리한다.
Compose가 SSM을 자동으로 읽지는 않으므로 주입 절차를 다음으로 고정한다.

1. EC2 IAM Role에 필요한 파라미터만 읽는 최소 권한(`ssm:GetParameter`) 부여
2. 배포 스크립트에서 `aws ssm get-parameter --with-decryption`으로 조회
3. `.env.runtime` 파일 생성 (gitignore, EC2 내부 권한 제한)
4. `docker compose --env-file .env.runtime up -d`

주의: 배포 로그에 secret 출력 금지, 컨테이너 env로 노출됨을 인지하고 접근 권한 관리.

### DB 백업 (Compose DB 사용 시 MVP 공개 전 필수)

같은 EC2의 Compose DB는 EC2 장애 = API+DB 동시 장애이고, 볼륨 삭제 실수·디스크 부족이 곧 데이터 소실이다.
UserAnswer/ScoringResult가 쌓이는 순간 데이터가 서비스 자산이므로 공개 전부터 다음을 갖춘다.

- MariaDB data volume 명시적 분리
- daily mysqldump → S3 업로드, 최소 7일 보관
- 복구 절차 문서화 (덤프에서 복원 리허설 1회 포함)
- EC2 EBS snapshot 주기 설정

### 빌드/이미지 (ARM64)

t4g는 Graviton(ARM64) 기반이므로 Docker 이미지의 `linux/arm64` 호환성을 확인한다.

- Spring Boot/MariaDB/Nginx 공식 이미지의 arm64 태그 확인
- CI(GitHub Actions)에서 amd64로만 빌드하면 EC2에서 실행 실패 — 초기에는 EC2에서 직접 build/pull, CI/CD로 확장 시 `linux/arm64` 빌드(buildx) 또는 ECR multi-arch push 명시

### 기타

- docker compose restart policy 설정 (`unless-stopped` 등)
- API health check endpoint 제공 (actuator 의존성 보유 — `/actuator/health` 확인 후 사용)
- CloudWatch Agent 또는 최소한의 로그 파일 관리 경로 정리

## LLM 연동(Phase 2)의 인프라 반영

LLM 호출로 채점 응답이 길어질 수 있으므로 1단계 구성에도 다음을 반영한다.

- Nginx `proxy_read_timeout`을 채점 요청 특성에 맞게 설정
- Spring LLM client timeout은 짧게, retry는 0~1회로 제한 (과한 retry는 사용자 대기 + 서버 스레드 점유)
- LLM 실패 시 Rule-only fallback 필수 — LLM 장애에도 채점 응답은 반환 (service-design.md "LLM 연동 구조" 원칙과 동일)

## 배포 전 선행 작업

- H2(인메모리) → MariaDB 전환. UserAnswer/ScoringResult가 재시작 후에도 보존되는지 확인
- seed data는 count==0 가드로 중복 삽입 방지 (기존 동작 유지 확인)
- CI(GitHub Actions test+build) 구축 후 배포 진행
- API health check endpoint 확인
- Nginx `/api/**` proxy와 SPA fallback이 충돌하지 않는지 설정 검증
