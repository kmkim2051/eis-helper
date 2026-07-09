package skyline.eis.eishelper.scoring.engine;

import skyline.eis.eishelper.problem.entity.Problem;
import skyline.eis.eishelper.scoring.entity.ScoringResult;

// 채점 한 건의 산출물. 저장된 결과(scoringResult)에 더해 매칭 상세(matchResult)와 채점 대상
// 문제(problem)를 함께 반환해, 제출 API가 키워드 목록과 추천 답안을 추가 조회 없이 구성할 수 있게 한다.
public record ScoringOutcome(
    Problem problem, ScoringResult scoringResult, KeywordMatchResult matchResult) {}
