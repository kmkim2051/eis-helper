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

## 2026-07-08 패키지 구조 결정 및 problem 도메인 JPA 엔티티 구현
- 변경: skyline.eis.eishelper.problem.entity 패키지에 Problem, ScoringRubric, ScoringCriterion, CriterionAlias 엔티티와 관련 enum(Subject, AnswerType, RubricGradingPolicy, CriterionImportance, CriterionGradingType) 추가
- 결정:
    - 패키지 구조는 레이어 기준(controller/{도메인}) 대신 도메인 기준(도메인/{controller,service,repository,dto,entity})으로 결정 — 도메인이 늘어날 로드맵(Phase 4, 6)을 고려해 응집도 우선
    - Problem/ScoringRubric/ScoringCriterion/CriterionAlias는 하나의 애그리거트(루트: Problem)로 보고 problem 패키지 하나에 묶음. 채점엔진(KeywordMatcher 등)은 CRUD 도메인이 아니므로 별도 scoring 패키지로 분리 예정
    - 연관관계 소유자: ScoringRubric→Problem(FK problem_id), ScoringCriterion→ScoringRubric(FK rubric_id), CriterionAlias→ScoringCriterion(FK criterion_id). Problem.scoringRubric은 mappedBy 읽기 전용 참조
    - cascade=ALL, orphanRemoval=true로 애그리거트 하위 엔티티는 루트 저장/삭제에 종속
    - createdAt/updatedAt은 Hibernate @CreationTimestamp/@UpdateTimestamp 사용 (별도 auditing 설정 없이 동작)
    - 엔티티는 @Getter + protected 기본생성자 + private 전체생성자 + @Builder 패턴, setter 없음
- 남은 것: problem 도메인 Repository/DTO/Service/Controller 구현 (문제 조회 API부터)

## 2026-07-08 scoring 도메인 JPA 엔티티 구현
- 변경: skyline.eis.eishelper.scoring.entity 패키지에 UserAnswer, ScoringResult, ScoringResultDetail 엔티티와 CriterionResultStatus enum 추가. docs/diagrams/domain-erd.md에 scoring 애그리거트 섹션 반영
- 결정:
    - UserAnswer(루트) 1:1 ScoringResult, ScoringResult 1:N ScoringResultDetail로 scoring 애그리거트 구성. cascade=ALL, orphanRemoval=true는 problem 애그리거트와 동일 패턴
    - scoring → problem 참조(UserAnswer.problemId, ScoringResultDetail.criterionId)는 애그리거트 경계를 넘으므로 JPA 연관관계 대신 Long id 컬럼만 보관
    - UserAnswer에는 score/totalScore를 두지 않고 ScoringResult에만 둠 (Problem/ScoringRubric의 totalScore 중복 제거와 같은 원칙)
    - ScoringResult.totalScore는 Problem.totalScore와 지금은 같은 값이지만 채점 시점 스냅샷이라 중복 아님(추후 Problem 배점이 바뀌어도 과거 결과는 채점 당시 배점 유지)으로 남기기로 함
    - UserAnswer.userId는 로그인 시스템이 없어 익명 식별자용 nullable 문자열로 유지, User 테이블은 만들지 않음
    - ScoringResultDetail.confidence는 LLM(SEMANTIC) 판정 항목만 값을 가지고 Rule 기반(KEYWORD) 항목은 null
- 남은 것: scoring 도메인 Repository/DTO/Service/Controller는 채점엔진(KeywordMatcher, ScoreCalculator 등) 구현 이후 진행 (roadmap Phase 1~2)
