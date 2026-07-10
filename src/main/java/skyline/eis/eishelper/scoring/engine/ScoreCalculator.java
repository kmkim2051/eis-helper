package skyline.eis.eishelper.scoring.engine;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;
import skyline.eis.eishelper.problem.entity.ScoringCriterion;
import skyline.eis.eishelper.scoring.entity.CriterionResultStatus;

// 채점 기준별 판정 상태(status)와 배점(score)으로 최종 점수를 계산한다.
// "AI가 아니라 서버가 점수를 계산한다"는 원칙의 실행 지점이므로, 상태 판정 방법(규칙/LLM)과는
// 분리해 criterionId->status 맵만 입력으로 받는다.
@Component
public class ScoreCalculator {

  private static final int PARTIAL_PERCENT = 50;

  public CalculatedScore calculate(
      List<ScoringCriterion> criteria,
      Map<Long, CriterionResultStatus> statusByCriterionId,
      int totalScore) {

    List<CriterionScore> criterionScores = new ArrayList<>();
    int rawTotal = 0;
    for (ScoringCriterion criterion : criteria) {
      CriterionResultStatus status =
          statusByCriterionId.getOrDefault(criterion.getId(), CriterionResultStatus.MISSING);
      int awarded = awardedScoreFor(criterion.getScore(), status);
      rawTotal += awarded;
      criterionScores.add(new CriterionScore(criterion.getId(), status, awarded));
    }
    // 총점 초과/음수 방지 안전장치. 정상 데이터라면 rawTotal은 이미 [0, totalScore] 안에 있다.
    int clampedTotal = Math.max(0, Math.min(rawTotal, totalScore));
    return new CalculatedScore(clampedTotal, totalScore, List.copyOf(criterionScores));
  }

  private int awardedScoreFor(int baseScore, CriterionResultStatus status) {
    return switch (status) {
      case MATCHED -> baseScore;
      case PARTIAL -> baseScore * PARTIAL_PERCENT / 100; // 부분 점수는 버림(floor)
      case MISSING, UNKNOWN -> 0; // UNKNOWN은 규칙으로 확정 불가 → 무득점(LLM 도입 전 보수적 처리)
      case NEGATIVE -> -baseScore; // 위험/오답 표현 감점 (이번 Phase 미사용, 정책만 선정의)
    };
  }
}
