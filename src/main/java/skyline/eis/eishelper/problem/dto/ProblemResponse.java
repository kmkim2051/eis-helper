package skyline.eis.eishelper.problem.dto;

import skyline.eis.eishelper.problem.entity.AnswerType;
import skyline.eis.eishelper.problem.entity.Problem;
import skyline.eis.eishelper.problem.entity.Subject;

// 채점 기준(rubric/criteria), 해설, 추천 답안은 풀이 전 정답 유출이므로 조회 응답에 포함하지 않는다.
public record ProblemResponse(
    Long id,
    Subject subject,
    String category,
    String questionText,
    int totalScore,
    AnswerType answerType) {

  public static ProblemResponse from(Problem problem) {
    return new ProblemResponse(
        problem.getId(),
        problem.getSubject(),
        problem.getCategory(),
        problem.getQuestionText(),
        problem.getTotalScore(),
        problem.getAnswerType());
  }
}
