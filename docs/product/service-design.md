# 시스템 설계 — 도메인 모델, 채점엔진, API

> 이 문서는 어떻게 만드는지(도메인 모델, 채점엔진 구조, AI 사용 원칙, API 초안)를 정의한다.
> 범위와 우선순위는 [mvp-scope-and-roadmap.md](mvp-scope-and-roadmap.md), 개발 시 주의사항은 [engineering-guidelines.md](engineering-guidelines.md) 참고.

## 1. 핵심 도메인 모델

초기 도메인 모델은 다음 개념을 중심으로 설계한다.

### Problem

문제 정보를 나타낸다.

주요 필드 예시: `id`, `subject`, `category`, `questionText`, `totalScore`, `answerType`, `difficulty`, `explanation`, `createdAt`, `updatedAt`

subject 예시:

- APPLICATION
- NETWORK
- SYSTEM
- GENERAL
- MANAGEMENT_LAW

초기 데이터는 APPLICATION만 제공하되, 구조적으로는 전과목 확장 가능하게 설계한다.

answerType 예시:

- DEFINITION
- LISTING
- PROCEDURE
- EXPLANATION
- COMPARISON
- MIXED

### ScoringRubric

문제별 채점 기준 묶음이다.

주요 필드 예시: `id`, `problemId`, `totalScore`, `gradingPolicy`, `description`

gradingPolicy 예시:

- KEYWORD_BASED
- SEMANTIC_BASED
- HYBRID

초기에는 HYBRID를 기본 방향으로 한다.
단, v1에서는 Rule 기반 키워드 매칭부터 시작하고, 이후 LLM 의미판정을 추가한다.

### ScoringCriterion

실제 채점 기준 항목이다.

주요 필드 예시: `id`, `rubricId`, `content`, `score`, `required`, `gradingType`, `orderNo`

예시 — 문제: SQL Injection 대응 방안을 3가지 쓰시오. [6점]

채점 기준:

- PreparedStatement 또는 바인딩 변수 사용: 2점
- 입력값 검증 또는 화이트리스트 검증: 2점
- DB 권한 최소화: 1점
- 에러 메시지 노출 제한: 1점
- WAF 적용: 1점

### CriterionAlias

채점 기준의 유사 표현을 관리한다.

주요 필드 예시: `id`, `criterionId`, `alias`

예시 — criterion: PreparedStatement 또는 바인딩 변수 사용

aliases:

- PreparedStatement
- prepared statement
- 프리페어드 스테이트먼트
- 바인딩 변수
- 파라미터 바인딩
- Parameterized Query
- SQL 문자열 결합 방지
- 쿼리 파라미터화

### UserAnswer

사용자가 제출한 답안이다.

주요 필드 예시: `id`, `userId`, `problemId`, `answerText`, `score`, `totalScore`, `submittedAt`

초기에는 로그인 없이 익명 사용자 기반으로 구현해도 된다.
다만 풀이 이력 분석을 위해 userId 또는 sessionId 개념은 고려할 수 있다.

### ScoringResult

답안 채점 결과다.

주요 필드 예시: `id`, `userAnswerId`, `score`, `totalScore`, `summary`, `createdAt`

### ScoringResultDetail

채점 기준별 상세 결과다.

주요 필드 예시: `id`, `scoringResultId`, `criterionId`, `status`, `awardedScore`, `reason`, `confidence`

status 예시:

- MATCHED
- PARTIAL
- MISSING
- NEGATIVE
- UNKNOWN

## 2. 채점엔진 설계

채점엔진은 이 프로젝트의 핵심이다.

중요한 원칙:

> AI에게 최종 점수를 직접 맡기지 않는다.
> AI는 "채점 기준 충족 여부 판단"과 "피드백 문장 생성"에 사용하고, 최종 점수 계산은 서버에서 수행한다.

### 전체 채점 흐름

1. 사용자가 답안을 제출한다.
2. 서버가 Problem과 ScoringRubric을 조회한다.
3. AnswerPreprocessor가 답안을 전처리한다.
4. KeywordMatcher가 정답 키워드와 유사 표현을 매칭한다.
5. Rule 기반으로 명확히 판정 가능한 항목을 먼저 처리한다.
6. 애매한 항목은 LlmCriterionEvaluator가 의미 충족 여부를 판단한다.
7. ScoreCalculator가 채점 기준에 따라 최종 점수를 계산한다.
8. FeedbackGenerator가 채점 결과 기반 피드백을 생성한다.
9. ScoringResult와 ScoringResultDetail을 저장한다.
10. 사용자에게 결과를 반환한다.

## 3. 채점엔진 내부 컴포넌트

### ScoringEngine

채점 전체 흐름을 오케스트레이션한다.

- 문제/채점기준 기반 채점 수행
- KeywordMatcher, LLM Evaluator, ScoreCalculator 호출
- 최종 ScoringResult 생성

### AnswerPreprocessor

사용자 답안을 정규화한다.

- 공백 정리
- 특수문자 정리
- 영문 대소문자 정규화
- 한글/영문 혼용 용어 정리
- 불필요한 조사/문장부호 제거
- 용어 표준화

예시: `PreparedStatement`, `prepared statement`, `프리페어드 스테이트먼트`, `바인딩 변수` — 이 표현들을 같은 의미군으로 처리할 수 있도록 전처리한다.

### KeywordMatcher

정답 키워드와 유사 표현을 매칭한다.

- 직접 키워드 매칭
- alias 기반 매칭
- 필수 키워드 누락 확인
- 오답/위험 표현 탐지

초기 MVP에서는 이 컴포넌트가 가장 중요하다.
처음부터 AI 의미판정에 의존하지 말고, Rule 기반 채점이 먼저 동작해야 한다.

### LlmCriterionEvaluator

AI를 사용해 애매한 표현이 채점 기준을 충족하는지 판단한다.

주의사항:

> AI에게 "몇 점인가?"를 묻지 않는다.
> AI에게는 "이 답안이 특정 채점 기준을 충족하는가?"만 묻는다.

입력 예시:

- 문제
- 채점 기준
- 사용자 답안
- 이미 매칭된 키워드
- 판단해야 할 criterion 목록

출력 예시:

```json
{
  "criterionId": 12,
  "status": "PARTIAL",
  "confidence": 0.78,
  "reason": "PreparedStatement를 직접 언급하지 않았지만 SQL 문자열을 직접 결합하지 않고 파라미터 방식으로 처리한다고 설명했으므로 부분 또는 의미상 인정 가능"
}
```

status는 다음 중 하나로 제한한다: `MATCHED`, `PARTIAL`, `MISSING`, `UNKNOWN`

### LLM 연동 구조 (Phase 2 결정, 2026-07-09)

Phase 2에서는 Spring AI 추상화보다 **Claude API 직접 호출**을 우선 사용한다.

이유: Phase 2의 LLM 사용 범위가 제한적이기 때문이다. 목표는 범용 챗봇이나 RAG가 아니라
Rule 기반 채점 결과 중 UNKNOWN 상태인 기준만 LLM으로 재판정하는 것이므로,
provider 교체 편의성보다 JSON 응답 제어, timeout/retry/fallback, status 제한,
confidence 처리, 예외 격리가 더 중요하다.

단, 애플리케이션 코드가 Claude API에 직접 의존하면 안 된다.
반드시 내부 LlmClient 인터페이스를 두고, Claude API 호출은 ClaudeLlmClient 구현체에 격리한다.

패키지 구조:

```
scoring/
  engine/
    LlmCriterionEvaluator
    ScoringEngine
    KeywordMatcher
    ScoreCalculator
  llm/
    LlmClient
    LlmClientProperties
    LlmClientException
  llm/claude/
    ClaudeLlmClient
    ClaudeRequestMapper
    ClaudeResponseParser
```

원칙:

1. LlmCriterionEvaluator는 Claude API를 직접 알면 안 된다.
2. LlmCriterionEvaluator는 LlmClient 인터페이스만 의존한다.
3. Claude API 장애, timeout, JSON 파싱 실패는 LlmClient 경계 안에서 도메인 예외 또는 실패 결과로 변환한다.
4. LLM 장애가 발생해도 ScoringEngine은 Rule 기반 결과만으로 채점을 계속해야 한다.
5. AI에게 최종 점수를 묻지 않는다.
6. AI는 criterion별 MATCHED, PARTIAL, MISSING, UNKNOWN 중 하나만 반환해야 한다.
7. 최종 점수 계산은 서버의 ScoreCalculator가 수행한다.

Spring AI는 Phase 2에서는 도입하지 않는다. 다만 추후 RAG, VectorStore, 다중 provider,
Advisor, 법규 근거 검색 등이 필요해지는 시점에 SpringAiLlmClient 구현체를 추가하는
방식으로 확장 가능하게 둔다.

### ScoreCalculator

점수 계산을 담당한다.

- MATCHED 항목에 full score 부여
- PARTIAL 항목에 partial score 부여
- MISSING 항목에 0점 부여
- NEGATIVE 항목은 감점 처리
- 총점 초과 방지
- 배점 합산

예시 정책:

- MATCHED: 100%
- PARTIAL: 50%
- MISSING: 0%
- NEGATIVE: 감점

최종 점수 계산은 반드시 서버 로직에서 수행한다.

### FeedbackGenerator

채점 결과를 바탕으로 사용자 피드백을 생성한다.

- 왜 감점되었는지 설명
- 인정된 키워드 안내
- 누락된 키워드 안내
- 시험형 추천 답안 생성
- 암기 포인트 제공
- 다음 복습 방향 제안

AI는 이 단계에서 적극적으로 활용할 수 있다.
단, 피드백은 ScoringResultDetail에 기반해야 하며, AI가 임의로 새로운 채점 근거를 만들어내면 안 된다.

## 4. AI 사용 원칙

### AI를 사용해도 되는 부분

- 의미 유사도 판단
- 애매한 답안의 기준 충족 여부 판단
- 피드백 문장 생성
- 시험형 답안 변환
- 누락 키워드 기반 보완 답안 생성
- 사용자가 이해하기 쉬운 해설 생성

### AI에게 맡기면 안 되는 부분

- 최종 점수 계산
- 전체 채점 기준 자동 생성
- 법/제도 최신성 판단
- 합격/불합격 단정
- 출처 없는 정답 확정
- 문제 데이터 무검증 생성

### 핵심 원칙

> AI가 판단한다.
> 서버가 검증한다.
> 서버가 점수화한다.
> AI가 설명한다.

## 5. API 설계 초안

### 문제 목록 조회

`GET /api/problems`

쿼리 파라미터 예시: `subject`, `category`, `difficulty`, `answerType`

응답 예시:

```json
[
  {
    "id": 1,
    "subject": "APPLICATION",
    "category": "SQL_INJECTION",
    "questionText": "SQL Injection 공격 원리와 대응 방안을 설명하시오.",
    "totalScore": 6,
    "difficulty": "MEDIUM"
  }
]
```

### 문제 상세 조회

`GET /api/problems/{problemId}`

응답 예시:

```json
{
  "id": 1,
  "subject": "APPLICATION",
  "category": "SQL_INJECTION",
  "questionText": "SQL Injection 공격 원리와 대응 방안을 설명하시오.",
  "totalScore": 6,
  "answerType": "EXPLANATION",
  "difficulty": "MEDIUM"
}
```

### 답안 제출 및 채점

`POST /api/answers/submit`

요청 예시:

```json
{
  "problemId": 1,
  "answerText": "PreparedStatement를 사용하고 입력값을 검증하며 에러 메시지를 숨긴다."
}
```

응답 예시:

```json
{
  "problemId": 1,
  "score": 5,
  "totalScore": 6,
  "matchedKeywords": ["PreparedStatement", "입력값 검증"],
  "partialKeywords": ["에러 메시지 노출 제한"],
  "missingKeywords": ["DB 권한 최소화", "WAF 적용"],
  "feedback": "핵심 대응 방안은 잘 작성했습니다. 다만 에러 메시지 노출 제한은 직접적인 SQL Injection 방어책보다는 보조 대책으로 보는 것이 적절합니다. DB 권한 최소화나 WAF 적용을 추가하면 더 안정적인 답안이 됩니다.",
  "recommendedAnswer": "SQL Injection은 입력값에 악의적인 SQL 구문을 삽입하여 DB를 비정상적으로 조회·변조하는 공격이다. 대응 방안으로는 PreparedStatement 또는 바인딩 변수 사용, 화이트리스트 기반 입력값 검증, DB 권한 최소화, 에러 메시지 노출 제한, WAF 적용 등이 있다."
}
```

### 풀이 이력 조회

`GET /api/answers/history`

응답 예시:

```json
[
  {
    "problemId": 1,
    "questionText": "SQL Injection 공격 원리와 대응 방안을 설명하시오.",
    "score": 5,
    "totalScore": 6,
    "submittedAt": "2026-07-07T16:00:00"
  }
]
```

### 취약점 분석 조회

`GET /api/analysis/weakness`

응답 예시:

```json
{
  "categoryScores": [
    { "category": "SQL_INJECTION", "averageScoreRate": 0.78 },
    { "category": "XSS", "averageScoreRate": 0.52 }
  ],
  "frequentlyMissingKeywords": ["출력값 인코딩", "HttpOnly", "CSP", "DB 권한 최소화"],
  "summary": "XSS 대응 방안에서 출력값 인코딩과 쿠키 보안 속성 관련 키워드 누락이 반복되고 있습니다."
}
```