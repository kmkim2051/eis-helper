package skyline.eis.eishelper.scoring.engine;

import java.util.List;

// ScoreCalculator의 최종 산출물. score는 [0, totalScore]로 제한된 합계,
// criterionScores는 기준별 원(raw) 배점 결과라 데이터 오류가 없으면 합이 score와 일치한다.
public record CalculatedScore(int score, int totalScore, List<CriterionScore> criterionScores) {}
