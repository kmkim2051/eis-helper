# TODO

## 진행 중

## 대기

Phase 1 우선순위 순 (위에서부터 착수):

- [ ] ScoringEngine 오케스트레이션 + 결과 저장 — Preprocessor→Matcher→Calculator 연결, UserAnswer/ScoringResult/ScoringResultDetail 저장
- [ ] 답안 제출 API 구현 — POST /api/answers/submit, 채점 결과 응답 (matched/partial/missing 키워드 + 예상 점수)
- [ ] 추천 답안 반환 — 제출 응답에 Problem.recommendedAnswer 포함
