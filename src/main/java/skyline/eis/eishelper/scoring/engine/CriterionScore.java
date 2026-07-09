package skyline.eis.eishelper.scoring.engine;

import skyline.eis.eishelper.scoring.entity.CriterionResultStatus;

// 채점 기준 하나에 대해 계산된 점수. reason/confidence는 판정 단계의 산출물이라 여기 담지 않는다.
public record CriterionScore(Long criterionId, CriterionResultStatus status, int awardedScore) {}
