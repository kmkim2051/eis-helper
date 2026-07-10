# CI 구축 설계서 (backend)

> 이 문서는 eis-helper(API 서버)의 CI 파이프라인 설계를 정의한다.
> 물리 배포 구조는 [system-architecture.md](system-architecture.md), 작업 이력은 docs/task/DONE.md 참고.
> 구현: `.github/workflows/ci.yml` (2026-07-10)

## 1. 목적과 원칙

- **배포 이전의 안전망**: main에 들어가는 코드는 "배포 가능"이 검증된 상태여야 한다.
- **검증 강도 차등**: 개발 루프(빠른 피드백)와 배포 관문(전체 검증)의 요구가 다르므로 트리거별로 job을 다르게 돌린다.
- **CI는 외부 시크릿 없이 동작**: 테스트가 H2 임베디드로 설계되어 있고, MariaDB 검증도 워크플로 내 서비스 컨테이너로 해결한다.

## 2. 브랜치 전략과 파이프라인 매핑

브랜치는 dev → main 2개. prd 브랜치는 두지 않는다 — 환경이 1개(운영 EC2)이고,
"배포 = 명시적 행위"는 v* 태그 + 수동 승인(CD)으로 달성한다.

| 단계 | 트리거 | 실행 job | 의미 |
|---|---|---|---|
| 1 | feature → dev PR, dev push | test | 빠른 피드백 (~2분) |
| 2 | dev → main PR | test + test-mariadb | **"배포 가능" 판정 지점** |
| 3 | main push (merge) | test + test-mariadb + build-candidate | 배포 후보 생성 |
| 4 | v* 태그 (예정) | CD workflow (미구현) | 운영 배포 |

## 3. Job 상세

### test — H2 테스트 (모든 트리거)

- JDK 21(temurin) + Gradle 캐시 → `./gradlew test`
- 테스트는 `src/test/resources/application.yml`에 의해 H2 임베디드로 실행 — 외부 DB 불필요
- `gradle/actions/wrapper-validation` 포함: gradle-wrapper.jar 변조 방지(공급망 안전장치)

### test-mariadb — 운영 DB 이식성 검증 (main행 PR / main push만)

- MariaDB 11 서비스 컨테이너를 **호스트 13306으로 매핑해 `application-local.yml`을 무수정 재사용**
  (`SPRING_PROFILES_ACTIVE=local ./gradlew test`)
- 존재 근거: H2만으로는 못 잡는 버그가 실제로 있었다 — @OneToMany 컬렉션 반환 순서가
  DB마다 달라 매칭 키워드 중복 제거가 흔들린 건(@OrderBy로 수정, DONE 2026-07-10).
  이 계열(방언·순서·타입 매핑) 회귀를 main 진입 전에 차단한다.
- CI의 MariaDB는 charset 플래그 없이 기본값 사용 — MariaDB 10.6+는 기본이 utf8mb4라 무해

### build-candidate — 배포 후보 생성 (main push만, 두 테스트 통과 후)

- `./gradlew bootJar` → GitHub Actions artifact 보관(14일)
- 이미지 저장소(ECR/GHCR) 도입 시 이 job을 **linux/arm64 이미지 빌드(buildx) + push로 승격**
  (운영이 t4g/Graviton이므로 amd64-only 빌드 금지 — system-architecture.md ARM64 원칙)

## 4. 공통 설정

- concurrency: 같은 ref의 연속 push 시 이전 실행 취소 (러너 낭비 방지)
- 모든 job `timeout-minutes: 10` — 행 방지
- 액션 버전은 메이저 태그 고정 (checkout@v4, setup-java@v4, upload-artifact@v4 등)

## 5. 브랜치 보호 (GitHub 저장소 설정 — 수동)

- main: PR 필수 + required status checks = `test`, `test-mariadb`
- dev: 직접 push 허용 (1인 개발 오버헤드 고려, push에도 CI가 돌아 안전망 유지)
- feature PR은 굵직한 작업 단위에 선택적으로 사용

## 6. CD 확장 설계 (EC2 구성 후 구현)

- 트리거: `v*` 태그 push
- 게이트: GitHub Environment("production") required reviewer로 수동 승인
- 동작: arm64 이미지 빌드 → registry push → EC2에서 pull + `docker compose up -d`
  (시크릿은 SSM Parameter Store 주입 절차 — system-architecture.md 참고)
- 1단계 운영은 무중단 배포를 목표로 하지 않는다 (배포 중 수초~수십 초 중단 허용)

## 7. 실패 정책

- red인 브랜치에는 merge하지 않는다. flaky 테스트가 생기면 재시도로 덮지 말고 원인을 고친다.
- main이 red가 되면(이론상 브랜치 보호로 차단되지만 hotfix 등 예외 시) 최우선으로 복구한다.
