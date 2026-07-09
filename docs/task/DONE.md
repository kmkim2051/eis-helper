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

## 2026-07-08 problem 시드 데이터 + totalScore 불변식 구현
- 변경:
    - src/main/resources/data/seed/problems.json 신규 (SQL Injection/XSS/CSRF 3문제, 문제당 기준 3~4개, 기준당 alias 3~6개, 전부 자체 작성 콘텐츠)
    - skyline.eis.eishelper.problem.seed 패키지에 ProblemSeeder(CommandLineRunner) + ProblemSeed/RubricSeed/CriterionSeed record 추가
    - skyline.eis.eishelper.problem.repository 패키지에 ProblemRepository, ScoringRubricRepository 신규 (problem 도메인 Repository 작업 일부 선구현)
    - build.gradle에 spring-boot-starter-json 추가, application.properties에 h2 콘솔/show-sql 설정 추가
- 결정:
    - 시드 파일은 저장소 루트 data/seed/가 아니라 src/main/resources/data/seed/에 둠 — 클래스패스(JAR 내부)에서 읽어야 하므로 resources 하위가 아니면 런타임에 못 찾음
    - Spring Boot 4.1.0은 Jackson 3(tools.jackson.* 패키지, groupId tools.jackson.core)로 이전됨 — ObjectMapper/TypeReference import가 기존 com.fasterxml.jackson.databind/core가 아니라 tools.jackson.databind/core임을 확인하고 반영
    - totalScore 불변식(criteria score 합 == Problem.totalScore) 검증은 ProblemSeeder.validateTotalScore()에 구현 — 세 시드 문제 모두 정확히 일치하도록 작성(시스템 설계 문서의 SQLi 예시는 5개 기준 합이 7점으로 6점 총점을 초과하는 예시라 이 불변식과 맞지 않아 그대로 쓰지 않고 자체 기준으로 재구성)
    - ScoringRubric은 mappedBy라 Problem.save()로 cascade되지 않음 — Problem을 먼저 save해 id를 받고, criteria/aliases를 채운 ScoringRubric을 ScoringRubricRepository로 별도 save해서 cascade=ALL로 하위까지 한 번에 저장
    - 시딩은 DB가 비어있을 때만 실행(problemRepository.count()==0 가드)해 재기동 시 중복 삽입 방지
- 검증: ./gradlew bootRun으로 실제 기동해 H2에 3문제 전체(Hibernate insert 로그)가 정상 삽입되는 것 확인, 에러 없이 기동 완료
- 남은 것: problem 도메인 DTO/Service/Controller 구현 (문제 조회 API)

## 2026-07-09 problem 도메인 문제 조회 API 구현
- 변경:
    - problem/dto/ProblemResponse(record), problem/service/ProblemService, problem/controller/ProblemController 신규 — GET /api/problems, GET /api/problems/{problemId}
    - common/exception 패키지 신규 — NotFoundException + GlobalExceptionHandler(@RestControllerAdvice, RFC 7807 ProblemDetail로 404 응답)
    - test에 ProblemControllerTest 신규 (@SpringBootTest + MockMvc 3케이스: 목록/상세/404)
- 결정:
    - 목록/상세 DTO는 현재 노출 가능 필드가 동일해 ProblemResponse 하나로 통일 (달라지는 시점에 분리)
    - 채점 기준(rubric/criteria), explanation, recommendedAnswer는 풀이 전 정답 유출이므로 조회 응답에서 제외 — 추천 답안/해설은 답안 제출 응답에서만 제공
    - subject/category 필터 쿼리 파라미터는 MVP에서 생략 (문제 3개뿐, 필요 시 추가)
    - 예외 응답 포맷은 Spring의 ProblemDetail(RFC 7807) 사용, 도메인 공통 NotFoundException은 common/exception에 배치
    - Spring Boot 4에서 @AutoConfigureMockMvc 패키지가 org.springframework.boot.test.autoconfigure.web.servlet → org.springframework.boot.webmvc.test.autoconfigure로 이동함 (Jackson 3 이동과 같은 계열의 Boot 4 모듈 재편)
- 검증: ./gradlew test 전체 통과(4개) + bootRun 실기동 후 curl로 목록/상세/404 응답 확인. 로컬 8080의 127.0.0.1은 다른 서비스(PID 4402)가 선점 중이라 [::1]:8080으로 확인함 — 추후 로컬 개발 시 server.port 변경 고려
- 남은 것: AnswerPreprocessor 구현 (채점엔진 파이프라인 첫 단계)

## 2026-07-09 AnswerPreprocessor 구현 (채점엔진 파이프라인 1단계)
- 변경:
    - scoring/engine/AnswerPreprocessor 신규 — 순수 함수 정적 메서드 preprocess(String), Spring 빈이 아닌 유틸리티 클래스(private 생성자)
    - test에 AnswerPreprocessorTest 신규 (순수 JUnit5 + AssertJ, 7케이스: 공백 정리, 대소문자 정규화, 특수문자 치환, 한글/영문/숫자 보존, null/blank 처리)
- 결정:
    - 이번 단계 범위는 TODO 기준대로 공백/대소문자/특수문자 정규화로 한정. 한글/영문 혼용 용어 정리·조사 제거·용어 표준화는 CriterionAlias 데이터로 이미 흡수되는 영역이라 다음 단계인 KeywordMatcher(alias 매칭)에서 처리하기로 함 — 여기서 규칙 기반 표준화를 시도하면 alias 데이터와 로직이 이중화될 위험
    - 특수문자는 빈 문자열이 아닌 공백으로 치환 (예: "SQL(공격)" → "sql 공격") — 구두점 제거 시 단어가 붙어버리는 것(예: "SQL공격") 방지
    - 허용 문자는 [0-9a-zA-Z가-힣\s] 화이트리스트 방식으로 결정 (제거할 특수문자를 나열하는 블랙리스트 방식보다 견고)
    - null 입력은 예외를 던지지 않고 빈 문자열 반환 — 이후 KeywordMatcher/ScoringEngine이 항상 non-null 문자열을 받도록 계약 단순화
    - MockMvc/@SpringBootTest 대신 순수 JUnit 단위테스트 사용 — Spring 컨텍스트 로딩 없이 빠르게 검증 가능한 순수 함수이므로
- 검증: ./gradlew test 전체 통과(11개, 신규 7개 포함)
- 남은 것: KeywordMatcher 구현 (직접 키워드 + alias 매칭)

## 2026-07-09 KeywordMatcher 구현 (채점엔진 파이프라인 2단계)
- 변경:
    - scoring/engine/KeywordMatcher(@Component) 신규 — 답안과 키워드를 AnswerPreprocessor로 동일 정규화 후 부분 문자열 포함 여부로 기준별 매칭
    - scoring/engine/KeywordMatch(record), KeywordMatchResult(record) 신규 — 기준별 매칭 결과 + CORE 누락 집계(missingCoreCriteria/hasMissingCoreCriteria)
    - test에 KeywordMatcherTest 신규 (순수 JUnit5 + AssertJ, 8케이스)
- 결정:
    - 키워드 후보는 CriterionAlias 목록을 사용하고, alias가 없는 기준은 content를 키워드로 폴백 — 데이터 모델상 '직접 키워드'용 별도 필드가 없고 alias가 그 역할을 겸함
    - 매칭 판정을 gradingType으로 분기: KEYWORD 기준은 미출현 시 규칙만으로 MISSING 확정, SEMANTIC/MANUAL 기준은 의미상 충족 가능성이 남아 UNKNOWN으로 두어 이후 LLM 판정 단계로 넘김. 즉 KeywordMatcher는 MATCHED/MISSING/UNKNOWN만 생성, PARTIAL/NEGATIVE는 만들지 않음(TODO 전제: NEGATIVE 데이터 소스 없음)
    - "CORE 기준 누락"은 MISSING으로 확정된 CORE만 집계 — SEMANTIC CORE는 미매칭이어도 UNKNOWN이라 누락으로 단정하지 않음(LLM이 인정할 여지)
    - 전처리 후 빈 문자열이 되는 키워드(예: "!!!")는 건너뜀 — "".contains("")가 항상 true라 발생하는 오탐 방지
    - 정규화는 공백을 보존하므로 "PreparedStatement"와 "prepared statement"는 서로 다른 문자열로 남음. 이는 버그가 아니라 alias 데이터로 두 표기를 모두 등록해 커버하는 설계(시드 데이터가 실제로 두 표기를 모두 포함)
    - KeywordMatcher는 @Component(파이프라인 스테이지), AnswerPreprocessor는 static util(순수 텍스트 함수)로 성격 구분 — 의존성 없어 단위테스트는 new로 직접 생성
- 검증: ./gradlew test 전체 통과(19개, 신규 8개 포함)
- 남은 것: ScoreCalculator 구현 (MATCHED/PARTIAL/MISSING별 배점 계산, 총점 초과 방지)

## 2026-07-09 ScoreCalculator 구현 (채점엔진 파이프라인 3단계)
- 변경:
    - scoring/engine/ScoreCalculator(@Component) 신규 — 기준별 status와 배점으로 최종 점수 계산
    - scoring/engine/CriterionScore(record), CalculatedScore(record) 신규 — 기준별 획득 점수 + 제한된 합계
    - test에 ScoreCalculatorTest 신규 (순수 JUnit5 + AssertJ, 7케이스)
- 결정:
    - 입력을 KeywordMatchResult가 아니라 criterionId->status 맵으로 받음 — 상태 판정(규칙/LLM)과 점수 계산을 분리해, LLM이 UNKNOWN을 MATCHED/PARTIAL로 갱신한 뒤 병합된 최종 상태 맵을 그대로 넘길 수 있게 함. calculator는 matcher/LLM 결과 형태에 의존하지 않음
    - 배점 정책: MATCHED=100%, PARTIAL=50%(버림/floor), MISSING/UNKNOWN=0, NEGATIVE=-배점. switch를 enum 5값에 대해 exhaustive하게 작성해 상태 추가 시 컴파일 강제
    - UNKNOWN은 0점 — 규칙만으로 충족을 확인할 수 없으므로 LLM 도입 전에는 무득점(보수적). LLM 단계에서 MATCHED/PARTIAL로 승격되면 그때 점수가 붙음
    - PARTIAL은 정수 나눗셈(score*50/100)으로 버림 처리 — 1점 기준 PARTIAL은 0점. 부분점수 반올림 인플레 방지
    - "총점 초과 방지"는 합계를 [0, totalScore]로 clamp하는 안전장치로 구현. 기준 배점 합이 총점과 어긋나는 데이터 오류 상황 대비이며, 정상 데이터에선 clamp 없이도 범위 내. 기준별 awardedScore는 raw로 보존(상세는 정직하게, 합계만 방어)
    - totalScore는 호출부(엔진)가 Problem/ScoringResult 스냅샷 값으로 넘기도록 파라미터화 — calculator가 Problem 엔티티에 직접 의존하지 않음
    - reason/confidence는 판정 단계 산출물이라 CriterionScore에 넣지 않음 — 엔진이 저장 시 KeywordMatch/LLM 결과와 병합
- 검증: ./gradlew test 전체 통과(26개, 신규 7개 포함)
- 남은 것: ScoringEngine 오케스트레이션 + 결과 저장 (Preprocessor→Matcher→Calculator 연결, UserAnswer/ScoringResult/ScoringResultDetail 저장)

## 2026-07-09 KeywordMatcher 토큰 기반 매칭 개선 (조사 매칭 갭 해결)
- 배경: 중간 검토에서 발견 — 기존 substring 매칭은 alias의 중간 단어에 조사가 붙으면 실패("입력값을 검증한다" vs "입력값 검증" → MISSING). 시드 alias 44개 중 40개가 다단어라 실사용 답안의 기본 경로에서 recall이 크게 떨어지고, KEYWORD 기준의 MISSING은 확정이라 Phase 2 LLM으로도 복구 불가한 구조였음
- 변경:
    - KeywordMatcher의 contains 판정을 두 전략의 OR로 교체: (1) 공백 제거 포함 매칭 — 붙여 쓴 답안("입력값검증")과 띄어쓰기 변형("prepared statement"↔"preparedstatement") 허용, (2) 토큰 접두어 매칭 — alias 토큰열이 답안의 연속 토큰열과 순서대로 접두어 일치하면 인정(조사/어미 허용)
    - KeywordMatcherTest에 5케이스 추가 (조사 매칭, 붙여쓰기 매칭, 한 표기 alias로 공백 변형 커버, 토큰 순서 불일치 거부, 토큰 사이 단어 삽입 거부)
- 결정:
    - 공백 제거 매칭은 기존 substring 매칭의 상위집합이라 회귀 없음 (공백 포함 매칭되던 것은 공백 제거 후에도 반드시 매칭됨)
    - 토큰 접두어는 "연속" 토큰만 인정, 사이에 다른 단어가 끼면("토큰 없이 검증") 불인정 — gap 허용 시 부정 표현 오탐 위험이 커서 보수적으로 결정. "노출하지 않도록 제한" 같은 삽입형 표현은 alias 추가나 Phase 2 LLM의 몫으로 남김
    - 형태소 분석기(예: 코모란/노리) 도입은 보류 — 접두어+공백제거 조합으로 조사/띄어쓰기 갭은 해소되고, 의존성 추가 대비 이득이 아직 없음
    - 이전 KeywordMatcher 결정 중 "PreparedStatement/prepared statement 두 표기를 alias로 모두 등록해 커버" 항목은 이번 변경으로 대체됨 — 공백 제거 매칭이 한 표기로 양쪽을 커버(기존 시드 alias는 그대로 둠, 중복이어도 무해)
- 검증: ./gradlew test 전체 통과(31개, 신규 5개 포함)
- 남은 것: ScoringEngine 오케스트레이션 + 결과 저장. 중간 검토 발견 2번(UNKNOWN의 제출 응답 표현 방식)은 엔진 단계에서 결정 필요
