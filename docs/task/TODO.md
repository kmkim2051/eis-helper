# TODO

## 진행 중

## 대기

Phase 1 우선순위 순 (위에서부터 착수):

- [ ] problem 도메인 DTO/Service/Controller 구현 — GET /api/problems, GET /api/problems/{id} (Repository는 시드 작업 때 선구현됨)
- [ ] AnswerPreprocessor 구현 — 공백/대소문자/특수문자 정규화, 순수 함수로 단위테스트 포함
- [ ] KeywordMatcher 구현 — 직접 키워드 + alias 매칭, CORE 기준 누락 판정 (Phase 1 핵심. NEGATIVE 판정은 데이터 소스가 없어 이번 Phase 미사용 전제)
- [ ] ScoreCalculator 구현 — MATCHED/PARTIAL/MISSING별 배점 계산, 총점 초과 방지
- [ ] ScoringEngine 오케스트레이션 + 결과 저장 — Preprocessor→Matcher→Calculator 연결, UserAnswer/ScoringResult/ScoringResultDetail 저장
- [ ] 답안 제출 API 구현 — POST /api/answers/submit, 채점 결과 응답 (matched/partial/missing 키워드 + 예상 점수)
- [ ] 추천 답안 반환 — 제출 응답에 Problem.recommendedAnswer 포함
