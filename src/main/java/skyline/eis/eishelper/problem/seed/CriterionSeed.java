package skyline.eis.eishelper.problem.seed;

import java.util.List;
import skyline.eis.eishelper.problem.entity.CriterionGradingType;
import skyline.eis.eishelper.problem.entity.CriterionImportance;

record CriterionSeed(
    String content,
    int score,
    CriterionImportance importance,
    CriterionGradingType gradingType,
    int orderNo,
    List<String> aliases) {}
