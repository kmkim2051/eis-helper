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

## 2026-07-09 ScoringEngine 오케스트레이션 + 결과 저장 (채점엔진 파이프라인 4단계)
- 변경:
    - scoring/engine/ScoringEngine(@Component, @Transactional) 신규 — 루브릭 조회 → KeywordMatcher → ScoreCalculator → UserAnswer/ScoringResult/ScoringResultDetail 저장
    - scoring/engine/ScoringOutcome(record) 신규 — 저장된 ScoringResult + KeywordMatchResult 묶음 반환
    - scoring/repository 패키지 신규 — UserAnswerRepository, ScoringResultRepository
    - ScoringRubricRepository에 findByProblemId 파생 쿼리 추가
    - test에 ScoringEngineTest 신규 (@SpringBootTest + @Transactional 롤백, 시드 문제 1 기반 5케이스)
- 결정:
    - UNKNOWN 표현(중간 검토 발견 2번): DB에는 UNKNOWN 그대로 저장해 Phase 2 LLM 재판정 대상을 식별 가능하게 유지. 사용자 제출 응답에서는 missing 목록에 합쳐 노출하기로 방침 결정(적용은 다음 단계 제출 API에서)
    - 엔진 반환은 엔티티가 아닌 ScoringOutcome — 제출 API가 matched/missing 키워드 목록을 매칭 재실행 없이 구성할 수 있도록 KeywordMatchResult를 함께 전달
    - criteria는 orderNo로 정렬 후 매칭/계산 — @OneToMany 컬렉션 순서가 보장되지 않으므로 상세 저장 순서를 결정적으로 만듦
    - 저장 idiom은 ProblemSeeder와 동일: UserAnswer 먼저 save → ScoringResult(details를 builder 백레퍼런스로 채움) save로 cascade
    - reason은 규칙 판정 근거를 한국어로 기록(MATCHED: "키워드 매칭: ...", MISSING: "매칭된 키워드 없음", UNKNOWN: "규칙 기반 판정 불가 — 의미 판정 대상"), confidence는 규칙 기반이라 전부 null
    - reasonFor의 switch는 exhaustive — 규칙 단계에서 PARTIAL/NEGATIVE가 나타나면 IllegalStateException (엔진 불변식 명시)
    - UserAnswer.userId는 null로 저장 (익명, 인증/세션 식별자 도입 시 확장). summary도 null (Phase 3 FeedbackGenerator 몫)
    - 없는 problemId는 rubric 조회 실패로 NotFoundException — problem/rubric을 각각 조회하지 않고 rubric 하나로 판정(1:1 필수 관계이므로)
- 검증: ./gradlew test 전체 통과(36개, 신규 5개 포함 — 채점/저장/reason 기록/0점+UNKNOWN·MISSING 구분/404)
- 남은 것: 답안 제출 API 구현 (POST /api/answers/submit — matched/partial/missing 키워드 + 예상 점수 + 추천 답안 응답, UNKNOWN→missing 합류 방침 적용)

## 2026-07-09 답안 제출 API 구현 (POST /api/answers/submit)
- 변경:
    - scoring/controller/AnswerController, scoring/service/AnswerService, scoring/dto/AnswerSubmitRequest·AnswerSubmitResponse 신규
    - build.gradle에 spring-boot-starter-validation 추가, 요청 검증은 Bean Validation(@NotNull problemId, @NotBlank answerText)
    - GlobalExceptionHandler에 MethodArgumentNotValidException 핸들러 추가 — 필드 오류를 모아 RFC 7807 ProblemDetail 400으로 응답
    - test에 AnswerControllerTest 신규 (@SpringBootTest + MockMvc + @Transactional 롤백, 5케이스: 정상 채점 응답/0점+UNKNOWN 합류/빈 답안 400/problemId 누락 400/없는 문제 404)
- 결정:
    - 응답 필드: problemId, score, totalScore, matchedKeywords, partialKeywords, missingKeywords (system-design 초안 준수). feedback/recommendedAnswer는 각각 Phase 3 / 다음 TODO 몫
    - matchedKeywords는 실제 인정된 alias 표기, partial/missingKeywords는 기준 content — 누락 안내는 개별 키워드보다 "어떤 기준을 못 채웠는지"가 유용하므로 기준 단위
    - UNKNOWN→missing 합류 방침 적용 지점이 여기(AnswerSubmitResponse.from). DB 저장은 UNKNOWN 구분 유지
    - partialKeywords는 Phase 1에서 항상 빈 배열이지만 응답 스키마 안정성을 위해 필드 유지 (Phase 2 LLM 도입 시 채워짐)
    - matchedKeywords는 정규화(공백 제거) 기준 중복 제거 — 공백 제거 매칭 도입으로 "PreparedStatement"/"prepared statement" 두 alias가 동시 매칭되어 표시가 중복되던 것을 첫 표기만 노출 (실기동 curl로 발견)
    - 상태코드는 200 — UserAnswer 생성이 일어나지만 API 의미가 "채점 수행과 결과 반환"이고 system-design 초안도 결과 응답 형태 (조회는 추후 /api/answers/history)
    - AnswerService는 요청/응답 변환만 담당, 트랜잭션은 ScoringEngine.score()가 소유
- 검증: ./gradlew test 전체 통과(41개, 신규 5개 포함) + bootRun(18080) 실기동 후 curl로 정상 채점(5/6점, 키워드 목록)/400/404 응답 확인
- 남은 것: 추천 답안 반환 (제출 응답에 Problem.recommendedAnswer 포함) — Phase 1 마지막 항목

## 2026-07-09 추천 답안 반환 (Phase 1 마지막 항목)
- 변경:
    - AnswerSubmitResponse에 recommendedAnswer 필드 추가, ScoringOutcome에 Problem 포함(엔진이 rubric으로 이미 로드한 Problem 재사용 — 추가 쿼리 없음)
    - AnswerControllerTest에 recommendedAnswer 검증 추가, ScoringEngineTest에 outcome.problem() 검증 추가
- 결정:
    - recommendedAnswer는 문제 조회 API가 아닌 제출 응답에서만 제공 (풀이 전 정답 유출 방지 원칙 유지)
    - AnswerService에서 ProblemRepository를 다시 조회하지 않고 ScoringOutcome에 Problem을 실어 전달 — 채점 트랜잭션 안에서 이미 로드된 엔티티 재사용
- 검증: ./gradlew test 전체 통과(41개) + bootRun(18080) 실기동 curl — XSS 문제(2번)에 조사 붙은 답안("출력값을 인코딩하고")을 제출해 토큰 매칭 인정 + recommendedAnswer 포함 응답 확인
- 남은 것: Phase 1 완료. 다음은 roadmap Phase 2 (LLM Client, LlmCriterionEvaluator, confidence/fallback 처리) — TODO 목록 재구성 필요

## 2026-07-10 H2 → MariaDB 전환 (배포 마일스톤 2)
- 변경:
    - docker-compose.yml 신규 — mariadb:11(utf8mb4 서버 고정, 한국어 시드 대응), named volume 분리, healthcheck, restart policy. Dockerize 단계에서 app 서비스 추가 예정
    - 설정을 application.yml 3분할로 구성 — 공통(spring.profiles.default=local) / application-local.yml(MariaDB 13306, ddl-auto=update) / application-prd.yml(env 주입 DB_URL 등, ddl-auto=validate, show-sql off). 운영 프로파일명은 'prod' 대신 'prd' 사용, 포맷은 properties 대신 yml
    - src/test/resources/application.yml 신규 — 테스트는 H2 임베디드 유지(테스트 클래스패스가 main 설정을 대체해 profiles.default 미적용)
    - ScoringCriterion.aliases에 @OrderBy("id ASC"), ScoringRubric.criteria에 @OrderBy("orderNo ASC") 추가
- 결정:
    - 호스트 포트는 13306 — 로컬 3306을 별도 네이티브 mariadbd(PID 4586)가 점유 중
    - 테스트 기본은 H2 유지 — CI에서 외부 DB 불필요, 빠름. MariaDB 호환 검증은 compose 기동 후 SPRING_PROFILES_ACTIVE=local ./gradlew test로 수행
    - compose의 자격증명은 로컬 전용으로 커밋 허용, 운영 값은 SSM → env 주입 (system-architecture.md 원칙)
    - h2-console 설정 제거 — MariaDB는 mysql 클라이언트로 조회
- 발견/수정 버그: MariaDB 테스트에서 matchedKeywords 중복 제거가 "prepared statement"를 남기고 "PreparedStatement"를 제거 — @OneToMany alias 컬렉션 순서가 DB 의존(H2 삽입 순, MariaDB 임의)인데 중복 제거가 "첫 표기 유지" 규칙이라 순서에 의존했음. @OrderBy로 등록 순서 고정해 해결 (H2만으로는 못 잡았을 이식성 버그)
- 로컬 환경 노트: Docker Desktop 데몬이 구버전(API 1.43)이라 신형 CLI(29.x)와 버전 협상 실패 — DOCKER_API_VERSION=1.43 환경변수 필요. Docker Desktop 업데이트 권장
- 검증: H2 테스트 41개 + MariaDB(local 프로파일) 테스트 41개 통과. 실기동(18080)으로 시딩 3문제 확인 → 답안 제출 → 재시작 → user_answer 보존 + 중복 시딩 없음 확인 (H2 시절 재시작 소실 문제 해소)
- 남은 것: 배포 마일스톤 계속 — CI 구축(1번), Dockerize(3번)

## 2026-07-10 CI 구축 (배포 마일스톤 1)
- 변경: .github/workflows/ci.yml 신규 (front repo에도 별도 작성), docs/product/ci-design.md 설계서 작성 (front repo에도 각각)
- 결정:
    - 브랜치 전략: dev → main 2-브랜치 + v* 태그. prd 브랜치는 만들지 않음 — 환경이 1개(운영 EC2 예정)이고 배포 자동화 전이라 브랜치 동기화 비용만 발생. "배포 = 명시적 행위"는 추후 CD에서 v* 태그 + GitHub Environment 수동 승인으로 달성
    - 검증 강도 차등: feature→dev PR/dev push는 test(H2)만(빠른 피드백), dev→main PR과 main push는 test-mariadb(서비스 컨테이너, 13306 매핑으로 application-local.yml 무수정 재사용) 추가 — "배포 가능" 판정이 main 진입 지점에서 일어나도록
    - main push 시 build-candidate job이 bootJar를 artifact로 보관(14일) — ECR 등 이미지 저장소가 생기면 linux/arm64 이미지 push로 승격
    - CD(v* 태그 → 운영 배포)는 EC2 구성 후 별도 workflow로 — 지금은 설계만 주석으로 남김
    - gradle wrapper 검증(gradle/actions/wrapper-validation) 포함 — wrapper.jar 변조 방지
    - CI용 MariaDB는 utf8mb4 command 플래그 없이 기본값 사용 — MariaDB 10.6+부터 기본 charset이 utf8mb4라 테스트에 영향 없음
- 검증: YAML 파싱 검증만 수행(js-yaml) — 실제 green 확인은 push 후 가능 (TODO에 후속 항목)
- 남은 것: workflow push 후 green 확인, main 브랜치 보호 설정(직접), Dockerize(마일스톤 3)

## 2026-07-10 CI 외부 피드백 검토 및 ci-guideline.md 작성
- 변경: docs/product/ci-guideline.md 신규 — CI/CD 개선 원칙(빌드 1회·동일 산출물 배포, 추적성, 최소 권한, CI 환경 명시, 버전 고정) + 적용 시점 구분
- 결정:
    - 즉시 적용 5건 수용: 브랜치 보호(기존 TODO), ci 프로파일 분리, MariaDB 버전 고정, *-plain.jar 제외(+artifact SHA 이름), permissions: contents: read → TODO 등록
    - Docker/CD 시점 적용 4건 수용: 테스트한 동일 이미지 배포, SHA/digest 추적, main concurrency 취소 분리(배포 후보 생성 중단 방지), AWS 인증 OIDC
    - test/integrationTest task 분리는 보류 — Redis/Kafka/Flyway 부재 + "같은 스위트를 두 엔진에 돌리는 교차 검증"이 의도된 설계(@OrderBy 버그를 잡은 방식). 테스트 수백 개 규모 또는 외부 인프라 의존 테스트 발생 시 재검토
    - paths-ignore는 조건부 보류 — required check와 조합 시 skip된 check가 pending으로 남아 merge가 막히는 함정을 문서화. CI ~2분이라 비용 문제 없음, 10분+ 시 재검토
- 검증: 문서 작업 (workflow 수정은 TODO "CI 개선 4건"으로 분리, 미적용)
- 남은 것: CI 개선 4건 적용, 브랜치 보호 설정(직접)

## 2026-07-10 CI 개선 4건 적용 (ci-guideline.md §2)
- 변경:
    - application-ci.yml 신설 — CI 서비스 컨테이너 전용 프로파일, workflow test-mariadb를 local→ci 프로파일로 교체 (로컬 편의 변경과 CI의 결합 해소)
    - MariaDB 버전 고정 — compose·CI 모두 mariadb:11 → mariadb:11.8 (로컬 컨테이너 실버전 11.8.8 확인 후 동일 마이너로 고정)
    - build.gradle에서 jar task 비활성화 — plain jar 생성 자체를 꺼서 artifact 오염 방지, artifact 이름에 SHA 포함(eis-helper-boot-<sha>)
    - workflow에 permissions: contents: read 명시
    - TODO에 문제 데이터 확충(3→20~30문제) 항목 추가 (roadmap에만 있고 TODO에 누락되어 있었음)
- 검증: bootJar 산출물이 단일 jar임을 확인, YAML 파싱 검증, SPRING_PROFILES_ACTIVE=ci로 전체 테스트 41개 통과(로컬 13306 컨테이너 = CI 구성과 동일), H2 기본 테스트도 통과
- 남은 것: push 후 dev→main PR에서 test-mariadb(ci 프로파일) green 확인, 브랜치 보호 설정(직접)

## 2026-07-10 배포 가이드라인 작성
- 변경: docs/product/deploy-guideline.md 신규 — CI→배포 연결 흐름, 배포 전/후 체크리스트, 수동 배포 절차(1단계), CD 전환 계획, 롤백 기준
- 결정:
    - 배포물은 main CI가 검증한 산출물 원칙 (front dist는 artifact 그대로 배치, backend 이미지는 초기 EC2 빌드 허용 후 registry 도입 시 digest 배포로 전환). bootJar는 JVM이라 아키텍처 중립, ARM64 제약은 Docker 이미지에만 해당함을 명시
    - 프로젝트 고유 주의사항 2건 문서화: (1) prd ddl-auto=validate라 스키마 변경 배포는 DDL 선적용 필수, 기동 실패가 의도된 신호 (2) 시딩 가드(count==0) 때문에 신규 문제가 배포로 반영 안 됨 — 문제 확충 착수 전 증분 반영 방식 결정 필요를 TODO 연계
    - DB는 롤백하지 않고 전방 수정(fix-forward) 원칙, 코드 롤백은 직전 태그 재배포
    - 롤백 판단 기준 명시: 스모크 3종 실패 또는 기동 실패 5분 초과
- 검증: 문서 작업
- 남은 것: 배포 마일스톤 3(Dockerize) 진행 시 이 문서의 §3 절차가 실제로 동작하는지 확인

## 2026-07-10 브랜치 보호(ruleset) 설정 및 첫 dev→main merge (사용자 직접)
- 변경: GitHub ruleset — main에 PR 필수 + required check(test, test-mariadb). 첫 dev→main PR merge로 main 동기화
- 검증: required check 하에 merge 성공 = test-mariadb(ci 프로파일) 첫 실행 green. main push로 build-candidate artifact 첫 생성
- 남은 것: 배포 마일스톤 3 (Dockerize)

## 2026-07-10 Dockerize (배포 마일스톤 3)
- 변경:
    - Dockerfile 신규 — 멀티스테이지(temurin 21 JDK 빌드 → JRE 실행), 의존성 레이어 분리 캐시, non-root(appuser) 실행. eclipse-temurin은 linux/arm64 지원(t4g 대응), 로컬(ARM Mac) 빌드 = 운영과 동일 아키텍처
    - docker-compose.yml에 app 서비스 추가 — SPRING_PROFILES_ACTIVE 기본 docker(env로 prd 오버라이드), DB_* 패스스루, 127.0.0.1:${APP_PORT:-18080} localhost bind, depends_on: mariadb healthy, restart: unless-stopped
    - application-docker.yml 신규 — compose 로컬 검증용 프로파일(사용자 결정). compose 네트워크의 mariadb:3306 접속, ddl-auto=update로 빈 DB에서도 기동. 운영은 prd + .env.runtime
    - .dockerignore 신규
- 결정:
    - compose 파일 하나로 로컬(docker 프로파일)/운영(prd + --env-file .env.runtime) 겸용 — 환경 차이는 env 변수로만 표현
    - 호스트 포트는 127.0.0.1:18080 bind (이 머신의 8080 점유 + 보안 원칙 '외부 노출은 Nginx만'), 운영에서는 APP_PORT=8080으로 오버라이드
    - 빈 볼륨 검증은 기존 데이터 볼륨을 건드리지 않도록 별도 compose 프로젝트(-p)의 일회용 볼륨으로 수행 후 삭제
- 검증: docker compose build/up으로 전체 스택 기동, 스모크 3종(목록 3문제/제출 3점+토큰매칭/404 ProblemDetail) 통과, non-root 확인. 빈 볼륨 신규 기동(~8초, 초기화→스키마 생성→시딩) 검증. 원래 스택 복구 후 기존 데이터(user_answer 2건) 무손실 확인
- 남은 것: 배포 마일스톤 4 (EC2 + Nginx 구성)
