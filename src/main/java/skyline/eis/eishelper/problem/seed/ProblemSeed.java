package skyline.eis.eishelper.problem.seed;

import skyline.eis.eishelper.problem.entity.AnswerType;
import skyline.eis.eishelper.problem.entity.Subject;

record ProblemSeed(
    Subject subject,
    String category,
    String questionText,
    int totalScore,
    AnswerType answerType,
    String explanation,
    String recommendedAnswer,
    RubricSeed rubric) {}
