# TODO

## 진행 중

## 대기

Front(eis-helper-front) 연동 — CORS는 사용하지 않는 구조로 결정 (2026-07-10):
로컬은 front의 vite proxy가 /api를 backend로 전달(same-origin), 운영은 프런트/백엔드를
같은 도메인으로 노출하고 리버스 프록시가 /api만 backend로 라우팅. backend 코드 변경 없음,
배포 구성(Nginx 등) 시점에 /api 라우팅으로 반영.

Phase 2 (Rule + LLM 하이브리드 채점) 우선순위 순 (위에서부터 착수):

- [ ] LLM Client 구현 — Claude API 직접 호출로 결정(Spring AI는 Phase 2 미도입, 결정 배경은 system-design.md "LLM 연동 구조" 참고). scoring/llm에 LlmClient 인터페이스 + LlmClientProperties + LlmClientException, scoring/llm/claude에 ClaudeLlmClient·ClaudeRequestMapper·ClaudeResponseParser로 Claude 의존 격리. API 키 환경변수 주입, timeout·retry 제한. Claude 장애/타임아웃/JSON 파싱 실패는 클라이언트 경계 안에서 도메인 예외 또는 실패 결과로 변환
- [ ] LlmCriterionEvaluator 구현 — LlmClient 인터페이스만 의존(Claude API 직접 참조 금지). UNKNOWN 기준만 판정 대상, 프롬프트에 문제/기준/답안/이미 매칭된 키워드 포함, 응답은 JSON 파싱 + 스키마 검증(criterionId/status/confidence/reason), status는 MATCHED·PARTIAL·MISSING·UNKNOWN으로 제한. "점수를 묻지 않는다" 원칙 준수. 테스트는 LlmClient mock으로 작성
- [ ] confidence 처리 — 기준치(예: 0.7) 미달 판정은 UNKNOWN으로 강등, LLM 판정 항목의 confidence/reason을 ScoringResultDetail에 저장 (Rule 판정 항목은 confidence null 유지)
- [ ] ScoringEngine에 LLM 판정 단계 통합 — KeywordMatcher와 ScoreCalculator 사이에서 UNKNOWN 재판정 후 criterionId→status 맵 병합. Rule 확정 결과(MATCHED/MISSING)는 LLM이 뒤집지 않음. LLM 호출 실패·타임아웃 시 규칙 기반 결과만으로 채점을 계속하는 fallback 필수 (LLM 장애에도 제출 응답은 반환)
- [ ] 제출 응답 PARTIAL 반영 검증 — LLM 판정 후 partialKeywords가 채워지고 50% 배점이 적용되는 흐름 e2e 확인, UNKNOWN→missing 합류가 LLM 판정 이후 상태 기준으로 동작하는지 확인
