package skyline.eis.eishelper.scoring.engine;

import skyline.eis.eishelper.scoring.entity.ScoringResult;

// 채점 한 건의 산출물. 저장된 결과(scoringResult)에 더해 매칭 상세(matchResult)를 함께 반환해,
// 제출 API가 matched/missing 키워드 목록을 재계산 없이 구성할 수 있게 한다.
public record ScoringOutcome(ScoringResult scoringResult, KeywordMatchResult matchResult) {}
