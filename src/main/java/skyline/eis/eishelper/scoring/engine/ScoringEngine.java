package skyline.eis.eishelper.scoring.engine;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import skyline.eis.eishelper.common.exception.NotFoundException;
import skyline.eis.eishelper.problem.entity.ScoringCriterion;
import skyline.eis.eishelper.problem.entity.ScoringRubric;
import skyline.eis.eishelper.problem.repository.ScoringRubricRepository;
import skyline.eis.eishelper.scoring.entity.CriterionResultStatus;
import skyline.eis.eishelper.scoring.entity.ScoringResult;
import skyline.eis.eishelper.scoring.entity.ScoringResultDetail;
import skyline.eis.eishelper.scoring.entity.UserAnswer;
import skyline.eis.eishelper.scoring.repository.ScoringResultRepository;
import skyline.eis.eishelper.scoring.repository.UserAnswerRepository;

// 채점 전체 흐름을 오케스트레이션한다: 루브릭 조회 → 키워드 매칭 → 점수 계산 → 결과 저장.
// Phase 2에서 LLM 판정이 추가되면 매칭과 계산 사이에서 UNKNOWN 기준의 status를 갱신하는 단계가 끼어든다.
@Component
@RequiredArgsConstructor
public class ScoringEngine {

  private final KeywordMatcher keywordMatcher;
  private final ScoreCalculator scoreCalculator;
  private final ScoringRubricRepository scoringRubricRepository;
  private final UserAnswerRepository userAnswerRepository;
  private final ScoringResultRepository scoringResultRepository;

  @Transactional
  public ScoringOutcome score(Long problemId, String answerText) {
    ScoringRubric rubric =
        scoringRubricRepository
            .findByProblemId(problemId)
            .orElseThrow(() -> new NotFoundException("문제의 채점 기준을 찾을 수 없습니다: " + problemId));
    List<ScoringCriterion> criteria =
        rubric.getCriteria().stream()
            .sorted(Comparator.comparingInt(ScoringCriterion::getOrderNo))
            .toList();

    KeywordMatchResult matchResult = keywordMatcher.match(answerText, criteria);
    Map<Long, CriterionResultStatus> statusByCriterionId =
        matchResult.matches().stream()
            .collect(Collectors.toMap(KeywordMatch::criterionId, KeywordMatch::status));
    CalculatedScore calculated =
        scoreCalculator.calculate(criteria, statusByCriterionId, rubric.getProblem().getTotalScore());

    ScoringResult scoringResult = saveResult(problemId, answerText, matchResult, calculated);
    return new ScoringOutcome(rubric.getProblem(), scoringResult, matchResult);
  }

  private ScoringResult saveResult(
      Long problemId, String answerText, KeywordMatchResult matchResult, CalculatedScore calculated) {
    UserAnswer userAnswer =
        userAnswerRepository.save(
            UserAnswer.builder().problemId(problemId).answerText(answerText).build());

    // summary는 Phase 3 FeedbackGenerator 도입 전까지 비워 둔다.
    ScoringResult scoringResult =
        ScoringResult.builder()
            .userAnswer(userAnswer)
            .score(calculated.score())
            .totalScore(calculated.totalScore())
            .build();

    Map<Long, KeywordMatch> matchByCriterionId =
        matchResult.matches().stream()
            .collect(Collectors.toMap(KeywordMatch::criterionId, Function.identity()));
    calculated
        .criterionScores()
        .forEach(
            criterionScore ->
                scoringResult
                    .getDetails()
                    .add(toDetail(scoringResult, criterionScore, matchByCriterionId.get(criterionScore.criterionId()))));

    return scoringResultRepository.save(scoringResult);
  }

  private ScoringResultDetail toDetail(
      ScoringResult scoringResult, CriterionScore criterionScore, KeywordMatch match) {
    // confidence는 LLM 판정 항목 전용이라 규칙 기반 단계에서는 항상 null.
    return ScoringResultDetail.builder()
        .scoringResult(scoringResult)
        .criterionId(criterionScore.criterionId())
        .status(criterionScore.status())
        .awardedScore(criterionScore.awardedScore())
        .reason(reasonFor(match))
        .build();
  }

  private String reasonFor(KeywordMatch match) {
    return switch (match.status()) {
      case MATCHED -> "키워드 매칭: " + String.join(", ", match.matchedKeywords());
      case MISSING -> "매칭된 키워드 없음";
      case UNKNOWN -> "규칙 기반 판정 불가 — 의미 판정 대상";
      case PARTIAL, NEGATIVE ->
          throw new IllegalStateException("규칙 기반 매칭 단계에서 나올 수 없는 상태: " + match.status());
    };
  }
}
