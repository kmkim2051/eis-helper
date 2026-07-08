package skyline.eis.eishelper.problem.seed;

import java.util.List;
import skyline.eis.eishelper.problem.entity.RubricGradingPolicy;

record RubricSeed(RubricGradingPolicy gradingPolicy, String description, List<CriterionSeed> criteria) {}
