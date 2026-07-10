# CI/CD 가이드라인

> 이 문서는 CI/CD의 **개선 원칙과 적용 시점**을 정의한다 (외부 피드백 검토 반영, 2026-07-10).
> 현재 파이프라인 구조는 [ci-design.md](ci-design.md), 배포 인프라는 [system-architecture.md](system-architecture.md) 참고.

## 1. 핵심 원칙

1. **한 번 빌드하고, 테스트한 그 산출물을 배포한다.** 테스트 통과한 바이너리/이미지와
   배포되는 것이 달라지는 순간 테스트의 보증이 무효가 된다. Docker 도입 후에는
   "테스트한 이미지의 digest = 배포 이미지의 digest"로 강제한다.
2. **산출물은 추적 가능해야 한다.** 어떤 artifact든 Git commit SHA로 역추적할 수 있어야 한다.
3. **workflow는 최소 권한으로 돈다.** 기본 `permissions: contents: read`, 필요한 job에만 추가 권한.
4. **CI 환경은 명시적으로 정의한다.** 개발자 로컬용 설정(local 프로파일)을 CI가 재사용하지 않는다 —
   로컬 편의 변경이 CI를 깨뜨리는 결합을 만들기 때문.
5. **버전은 고정한다.** DB 이미지, 액션, 툴체인 모두 운영과 동일하거나 명시된 버전 사용.

## 2. 즉시 적용 항목 (실효성 우선 5건)

| # | 항목 | 내용 |
|---|---|---|
| 1 | main 브랜치 보호 | PR 필수 + required checks(`test`, `test-mariadb`) — GitHub 설정 (docs/task/TODO.md 등록됨) |
| 2 | ci 프로파일 분리 | `application-ci.yml` 신설, CI의 MariaDB job은 local이 아닌 ci 프로파일 사용. local은 개발자 전용으로 되돌림 |
| 3 | MariaDB 버전 고정 | `mariadb:11`(움직이는 태그) → 운영 채택 버전과 동일한 마이너 태그(예: `mariadb:11.4`)로 compose·CI 통일 |
| 4 | plain jar 제외 | artifact 업로드에서 `*-plain.jar` 제외 (또는 gradle `jar` task 비활성화로 생성 자체를 끔) |
| 5 | workflow 최소 권한 | 워크플로 상단 `permissions: contents: read` 명시 |

추가 저비용 항목: artifact 이름에 Git SHA 포함 (`eis-helper-boot-${{ github.sha }}`) — run 페이지 밖에서 다운로드된 jar도 커밋 역추적 가능.

## 3. Docker/CD 도입 시 적용 항목

- **테스트한 동일 Docker 이미지를 배포한다** — 이미지 빌드 → 그 이미지로 테스트(또는 테스트 후 즉시 태깅) → 같은 digest를 push/배포. 재빌드 금지
- **이미지 태그는 Git SHA 기반** (`:sha-<short>`), 배포 기록은 digest로 남김
- **main의 concurrency 취소 정책 분리** — PR/dev는 cancel-in-progress로 러너 절약, main push는 취소 없이 완주 (배포 후보 생성이 후속 머지에 의해 중단되면 안 됨)
- **AWS 인증은 GitHub OIDC** — 장기 액세스 키를 시크릿에 넣지 않고, OIDC federation + 최소 권한 IAM Role 사용

## 4. 검토 후 보류/조건부 항목 (재검토 시점 명시)

### test / integrationTest task 분리 — 보류

단위 테스트와 통합 테스트(실 DB, Flyway 등)를 별도 gradle task로 나누는 표준 관행은 공감하나:

- 현재 이 프로젝트에는 Redis/Kafka/Flyway가 없고 전체 스위트가 수 초에 끝난다.
- **"같은 스위트를 H2와 MariaDB 두 엔진에 돌리는 것"은 의도된 설계다** — 동일 테스트의 결과 차이가
  곧 이식성 버그 신호이며, 실제로 @OneToMany 컬렉션 순서 버그(@OrderBy 건)를 이 방식으로 잡았다.
  task를 분리해 스위트가 달라지면 이 교차 검증이 사라진다.

재검토 시점: 테스트가 수백 개 규모가 되어 H2 job이 느려지거나, 외부 인프라(Redis 등) 의존 테스트가 생길 때.

### paths-ignore (docs 변경 시 CI 생략) — 조건부

- 함정: required check와 조합하면 path 필터로 skip된 check가 PR에서 pending으로 남아 **merge가 막힌다.**
  도입한다면 skip 시에도 성공으로 보고하는 패턴(별도 통과 job 등)이 함께 필요하다.
- 현재 CI는 ~2분이고 실행 비용이 문제되지 않으므로 도입하지 않는다.

재검토 시점: CI 소요가 10분+ 또는 러너 비용이 가시화될 때.

## 5. 이 문서의 위치

- 새 워크플로/파이프라인 변경 시 이 문서의 원칙(§1)과 충돌하는지 먼저 확인한다.
- 적용 항목이 구현되면 ci-design.md에 반영하고, 이 문서에서는 완료 표시 대신 원칙만 유지한다.
