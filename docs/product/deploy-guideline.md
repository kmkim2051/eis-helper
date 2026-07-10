# 배포 가이드라인

> 이 문서는 CI와 연결된 실제 배포 계획·체크리스트·주의사항을 정의한다.
> 인프라 구조는 [system-architecture.md](system-architecture.md)(1단계: EC2+Nginx+Compose),
> CI 파이프라인은 [ci-design.md](ci-design.md), 개선 원칙은 [ci-guideline.md](ci-guideline.md) 참고.
> front 정적 파일 배치도 이 문서가 다룬다 (운영 웹서버가 backend EC2의 Nginx이므로).

## 1. CI → 배포 연결 흐름

```
dev → main PR          : test(H2) + test-mariadb(ci 프로파일) 통과 = "배포 가능" 판정
main merge (push)      : build-candidate가 배포 후보 생성
                          - backend: eis-helper-boot-<sha> (bootJar artifact)
                          - front:  eis-helper-front-dist-<sha> (dist artifact)
v* 태그 + 배포 실행     : 1단계는 수동 절차(§3), CD 도입 후 자동(§6)
```

- 배포되는 것은 **main CI가 검증·생성한 산출물**이어야 한다 (빌드 1회 원칙).
- 예외: backend Docker 이미지는 초기에 EC2에서 직접 build 허용(ARM64 네이티브) —
  registry(ECR/GHCR) 도입 시점에 "CI가 빌드한 이미지 digest 배포"로 전환한다.
  참고로 bootJar 자체는 JVM 바이트코드라 아키텍처 중립이며, ARM64 제약은 Docker 이미지에만 해당한다.

## 2. 배포 전 체크리스트

- [ ] main CI green — `test`, `test-mariadb`, `build-candidate` 전부 성공
- [ ] 배포 버전 결정 — `v<major>.<minor>.<patch>` 태그명과 변경 요약 준비
- [ ] **스키마 변경 여부 확인** — 엔티티가 바뀌었으면 §5-1 절차 선행 (prd는 ddl-auto=validate라 스키마 불일치 시 기동 실패)
- [ ] **시드/문제 데이터 변경 여부 확인** — 새 문제 추가는 자동 반영되지 않음, §5-2 절차 필요
- [ ] 시크릿/환경변수 변경 여부 — 변경 시 SSM Parameter Store 먼저 갱신
- [ ] DB 백업 최신 확인 + **배포 직전 수동 dump 1회** (`mysqldump` → S3)
- [ ] 배포 창 확인 — 무중단 배포가 아니므로(수초~수십 초 중단) 사용자 적은 시간대 권장

## 3. 배포 절차 (1단계 — 수동)

1. **백업**: 배포 직전 mysqldump → S3 업로드 확인
2. **(스키마 변경 시)** DDL을 운영 DB에 적용 (§5-1)
3. **backend**: EC2에서 배포 대상 태그 checkout → `docker compose build app` →
   `docker compose up -d app` (MariaDB 컨테이너는 재생성하지 않음 — 데이터 볼륨 보호)
4. **front**: main CI의 `eis-helper-front-dist-<sha>` artifact 다운로드 →
   Nginx 정적 경로(/var/www/app)의 내용 교체 (재빌드 금지 — ci-guideline 원칙)
5. **헬스체크**: `curl https://<domain>/actuator/health` → UP 확인
6. **스모크 테스트** (§4의 필수 3종)
7. **배포 기록**: 배포한 커밋에 `v*` 태그 push, 배포 시각·SHA 기록

## 4. 배포 후 체크리스트

필수 스모크 3종 (핵심 루프):
- [ ] `GET /api/problems` — 문제 목록 정상 (개수 확인)
- [ ] 화면에서 문제 풀이 → 제출 → 예상 점수/키워드/추천 답안 표시
- [ ] 존재하지 않는 경로/문제 — SPA fallback과 404(ProblemDetail) 각각 정상

추가 확인:
- [ ] 애플리케이션 로그에 기동 에러/스택트레이스 없음
- [ ] (스키마 변경 배포였다면) validate 통과 = 스키마 일치 확인된 것
- [ ] 이상 시 §7 롤백 판단 — "일단 지켜보기"보다 빠른 롤백이 낫다

## 5. 이 프로젝트 고유의 배포 주의사항

### 5-1. 스키마 변경 배포 (ddl-auto=validate)

운영은 `validate`라 Hibernate가 스키마를 만들거나 고치지 않는다 — **엔티티 변경이 포함된
배포는 DDL을 먼저 운영 DB에 적용해야 하며, 누락 시 기동 실패가 의도된 동작**이다(드리프트 조기 발견).

- 절차: 로컬 MariaDB에서 `ddl-auto=update`로 생성된 스키마 diff 확인 → DDL 스크립트 작성 →
  운영 적용 → 배포
- 스키마 변경이 잦아지면 Flyway 도입을 검토한다 (마이그레이션 이력 관리 — 현재 미도입)

### 5-2. 문제 데이터 추가는 배포로 반영되지 않는다

ProblemSeeder는 `count()==0`일 때만 동작한다. **운영 DB에 데이터가 생긴 후에는
problems.json에 문제를 추가해도 배포만으로는 반영되지 않는다.**

- 당분간: 신규 문제는 운영 DB에 수동 INSERT (또는 임시 스크립트)
- 근본 해결: Phase 5(문제/채점기준 관리 기능) 또는 시딩 로직의 증분 반영 개선 — 문제 확충
  작업(TODO) 시작 전에 이 중 하나를 결정할 것

### 5-3. 기타

- **MariaDB 컨테이너는 배포 대상이 아니다** — `docker compose up`시 app 서비스만 재기동,
  DB 컨테이너/볼륨을 건드리는 명령(`down -v` 등) 금지
- **MariaDB 버전 업그레이드는 별도 작업** — compose 태그(11.8) 변경은 코드 배포와 분리하고,
  업그레이드 전 백업 + CI 이미지 버전 동시 변경
- **Nginx 설정 변경**은 `nginx -t`로 검증 후 reload (restart 불필요, 무중단)
- Phase 2(LLM) 이후: LLM API 키는 SSM으로만 주입, 배포 후 스모크에 "LLM 장애 시에도
  채점 응답 반환(fallback)" 확인 추가

## 6. CD 전환 계획 (운영 안정 후)

전환 조건: 1단계 수동 배포가 반복되어 절차가 검증되고, registry(ECR/GHCR)가 준비된 시점.

- 트리거: `v*` 태그 push → CD workflow
- 게이트: GitHub Environment("production") required reviewer 수동 승인
- 동작: main CI가 빌드한 **linux/arm64 이미지 digest**를 EC2에서 pull + up (재빌드 없음)
- 인증: GitHub OIDC + 최소 권한 IAM Role (장기 액세스 키 금지)
- main의 concurrency는 취소 없이 완주하도록 분리 (ci-guideline §3)

## 7. 롤백

- **코드 롤백**: 직전 정상 태그로 §3 절차 재수행 (front dist도 해당 SHA artifact로)
- **DB는 롤백하지 않는 것이 원칙** — 스키마를 되돌리면 신규 데이터가 깨진다.
  전방 수정(fix-forward)을 우선하고, dump 복원은 데이터 파손 시 최후 수단
  (복원 절차는 백업 구성 시 문서화 + 리허설 — system-architecture.md 백업 원칙)
- 롤백 판단 기준: 스모크 3종 중 하나라도 실패, 또는 기동 실패가 5분 내 해결되지 않을 때
