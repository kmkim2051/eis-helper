# DONE

## 2026-07-07 프로젝트 초기 세팅
- 변경: Spring Boot 4.1.0 + Java 21 프로젝트 생성 (Gradle, JPA/JDBC, Lombok, H2/MariaDB)
- 결정: Claude 스킬 4개 도입 — /task(작업 관리), /catch-up(세션 요약), /oop-check(리팩토링 점검), /api-review(API 설계 점검)
- 남은 것: 없음

## 2026-07-07 Phase 1 도메인 ERD 설계
- 변경: docs/diagrams/domain-erd.md 신규 작성 (Problem, ScoringRubric, ScoringCriterion, CriterionAlias)
- 결정:
    - Problem : ScoringRubric = 1:1로 고정 (버전 관리 필요해지면 1:N으로 확장)
    - totalScore는 Problem에만 두고 ScoringRubric에는 중복 저장하지 않음 (단일 진실 소스)
    - difficulty 필드는 채점 로직에 쓰이지 않고 사람마다 기준이 달라 제외
    - subject는 지금은 단순 문자열/enum 유지, 별도 Subject 테이블 정규화(과목명 이력·개정연도)는 과목 확장 시점(roadmap Phase 6)으로 보류
    - SCORING_CRITERION.required(boolean) → importance(CORE/NORMAL/OPTIONAL) enum으로 변경
    - gradingPolicy(RubricGradingPolicy)와 gradingType(CriterionGradingType) enum 분리, CriterionGradingType에 MANUAL(자동 판정 불가, 관리자 수동 채점) 추가
    - CRITERION_ALIAS는 (criterionId, alias) 복합 unique 제약으로 같은 기준 내 중복 alias만 방지 (전역 unique 아님)
- 남은 것: 이 ERD 기반 JPA 엔티티 구현 (Phase 1 1순위)
