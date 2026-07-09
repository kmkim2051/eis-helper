package skyline.eis.eishelper.scoring.engine;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import skyline.eis.eishelper.problem.entity.ScoringCriterion;
import skyline.eis.eishelper.scoring.entity.CriterionResultStatus;

class ScoreCalculatorTest {

  private final ScoreCalculator scoreCalculator = new ScoreCalculator();

  @Test
  void 모든_기준이_MATCHED면_배점_합계를_받는다() {
    List<ScoringCriterion> criteria = List.of(criterion(1L, 2), criterion(2L, 2), criterion(3L, 2));
    Map<Long, CriterionResultStatus> statuses =
        Map.of(
            1L, CriterionResultStatus.MATCHED,
            2L, CriterionResultStatus.MATCHED,
            3L, CriterionResultStatus.MATCHED);

    CalculatedScore result = scoreCalculator.calculate(criteria, statuses, 6);

    assertThat(result.score()).isEqualTo(6);
    assertThat(result.totalScore()).isEqualTo(6);
  }

  @Test
  void MISSING과_UNKNOWN은_0점_처리된다() {
    List<ScoringCriterion> criteria = List.of(criterion(1L, 2), criterion(2L, 2));
    Map<Long, CriterionResultStatus> statuses =
        Map.of(1L, CriterionResultStatus.MISSING, 2L, CriterionResultStatus.UNKNOWN);

    CalculatedScore result = scoreCalculator.calculate(criteria, statuses, 4);

    assertThat(result.score()).isZero();
    assertThat(result.criterionScores())
        .extracting(CriterionScore::awardedScore)
        .containsExactly(0, 0);
  }

  @Test
  void PARTIAL은_배점의_50퍼센트를_버림으로_받는다() {
    List<ScoringCriterion> criteria = List.of(criterion(1L, 4), criterion(2L, 3), criterion(3L, 1));
    Map<Long, CriterionResultStatus> statuses =
        Map.of(
            1L, CriterionResultStatus.PARTIAL,
            2L, CriterionResultStatus.PARTIAL,
            3L, CriterionResultStatus.PARTIAL);

    CalculatedScore result = scoreCalculator.calculate(criteria, statuses, 8);

    // 4->2, 3->1(버림), 1->0(버림)
    assertThat(result.criterionScores())
        .extracting(CriterionScore::awardedScore)
        .containsExactly(2, 1, 0);
    assertThat(result.score()).isEqualTo(3);
  }

  @Test
  void 배점_합이_총점을_넘어도_총점으로_제한된다() {
    List<ScoringCriterion> criteria = List.of(criterion(1L, 4), criterion(2L, 4));
    Map<Long, CriterionResultStatus> statuses =
        Map.of(1L, CriterionResultStatus.MATCHED, 2L, CriterionResultStatus.MATCHED);

    CalculatedScore result = scoreCalculator.calculate(criteria, statuses, 6);

    assertThat(result.score()).isEqualTo(6);
  }

  @Test
  void NEGATIVE_감점으로_합이_음수가_되어도_0으로_제한된다() {
    List<ScoringCriterion> criteria = List.of(criterion(1L, 3));
    Map<Long, CriterionResultStatus> statuses = Map.of(1L, CriterionResultStatus.NEGATIVE);

    CalculatedScore result = scoreCalculator.calculate(criteria, statuses, 6);

    assertThat(result.score()).isZero();
    assertThat(result.criterionScores().get(0).awardedScore()).isEqualTo(-3);
  }

  @Test
  void 상태맵에_없는_기준은_MISSING으로_처리된다() {
    List<ScoringCriterion> criteria = List.of(criterion(1L, 2), criterion(2L, 2));
    Map<Long, CriterionResultStatus> statuses = Map.of(1L, CriterionResultStatus.MATCHED);

    CalculatedScore result = scoreCalculator.calculate(criteria, statuses, 4);

    assertThat(result.criterionScores())
        .extracting(CriterionScore::criterionId, CriterionScore::status, CriterionScore::awardedScore)
        .containsExactly(
            tuple(1L, CriterionResultStatus.MATCHED, 2),
            tuple(2L, CriterionResultStatus.MISSING, 0));
    assertThat(result.score()).isEqualTo(2);
  }

  @Test
  void 혼합_판정_시_기준별_점수와_합계가_정확히_계산된다() {
    // SQL Injection 루브릭 유사: 원리 2 + PreparedStatement 2 + 입력검증 1 + 권한 1 = 6점 만점
    List<ScoringCriterion> criteria =
        List.of(criterion(1L, 2), criterion(2L, 2), criterion(3L, 1), criterion(4L, 1));
    Map<Long, CriterionResultStatus> statuses =
        Map.of(
            1L, CriterionResultStatus.MATCHED,
            2L, CriterionResultStatus.MATCHED,
            3L, CriterionResultStatus.MISSING,
            4L, CriterionResultStatus.MISSING);

    CalculatedScore result = scoreCalculator.calculate(criteria, statuses, 6);

    assertThat(result.score()).isEqualTo(4);
    assertThat(result.criterionScores())
        .extracting(CriterionScore::awardedScore)
        .containsExactly(2, 2, 0, 0);
  }

  private ScoringCriterion criterion(Long id, int score) {
    return ScoringCriterion.builder().id(id).score(score).build();
  }
}
