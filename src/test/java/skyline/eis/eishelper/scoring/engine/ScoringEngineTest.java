package skyline.eis.eishelper.scoring.engine;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import skyline.eis.eishelper.common.exception.NotFoundException;
import skyline.eis.eishelper.scoring.entity.CriterionResultStatus;
import skyline.eis.eishelper.scoring.entity.ScoringResult;
import skyline.eis.eishelper.scoring.entity.ScoringResultDetail;
import skyline.eis.eishelper.scoring.repository.ScoringResultRepository;
import skyline.eis.eishelper.scoring.repository.UserAnswerRepository;

// 시드 문제 1(SQL Injection, 6점: 원리 SEMANTIC 2 + PreparedStatement 2 + 입력값 검증 1 + DB권한/에러 1) 기준.
@SpringBootTest
@Transactional
class ScoringEngineTest {

  @Autowired private ScoringEngine scoringEngine;
  @Autowired private UserAnswerRepository userAnswerRepository;
  @Autowired private ScoringResultRepository scoringResultRepository;

  @Test
  void 답안을_채점하면_점수와_기준별_상세가_계산되어_저장된다() {
    ScoringOutcome outcome =
        scoringEngine.score(1L, "입력값에 SQL 구문을 삽입하는 공격이다. PreparedStatement를 사용하고 입력값을 검증한다.");

    assertThat(outcome.problem().getId()).isEqualTo(1L);
    ScoringResult result = outcome.scoringResult();
    assertThat(result.getId()).isNotNull();
    assertThat(result.getScore()).isEqualTo(5);
    assertThat(result.getTotalScore()).isEqualTo(6);
    assertThat(result.getDetails())
        .extracting(ScoringResultDetail::getStatus, ScoringResultDetail::getAwardedScore)
        .containsExactly(
            tuple(CriterionResultStatus.MATCHED, 2),
            tuple(CriterionResultStatus.MATCHED, 2),
            tuple(CriterionResultStatus.MATCHED, 1),
            tuple(CriterionResultStatus.MISSING, 0));
  }

  @Test
  void UserAnswer와_ScoringResult가_DB에_저장된다() {
    long answersBefore = userAnswerRepository.count();

    ScoringOutcome outcome = scoringEngine.score(1L, "PreparedStatement를 사용한다");

    assertThat(userAnswerRepository.count()).isEqualTo(answersBefore + 1);
    ScoringResult saved =
        scoringResultRepository.findById(outcome.scoringResult().getId()).orElseThrow();
    assertThat(saved.getUserAnswer().getProblemId()).isEqualTo(1L);
    assertThat(saved.getUserAnswer().getAnswerText()).isEqualTo("PreparedStatement를 사용한다");
    assertThat(saved.getDetails()).hasSize(4);
  }

  @Test
  void 매칭된_기준의_reason에는_매칭_키워드가_기록된다() {
    ScoringOutcome outcome = scoringEngine.score(1L, "바인딩 변수를 사용한다");

    ScoringResultDetail matchedDetail =
        outcome.scoringResult().getDetails().stream()
            .filter(detail -> detail.getStatus() == CriterionResultStatus.MATCHED)
            .findFirst()
            .orElseThrow();
    assertThat(matchedDetail.getReason()).contains("바인딩 변수");
    assertThat(matchedDetail.getConfidence()).isNull();
  }

  @Test
  void 아무_기준도_충족하지_않으면_0점이고_SEMANTIC은_UNKNOWN_KEYWORD는_MISSING으로_저장된다() {
    ScoringOutcome outcome = scoringEngine.score(1L, "정보보안기사 자격증 공부");

    ScoringResult result = outcome.scoringResult();
    assertThat(result.getScore()).isZero();
    assertThat(result.getDetails())
        .extracting(ScoringResultDetail::getStatus)
        .containsExactly(
            CriterionResultStatus.UNKNOWN,
            CriterionResultStatus.MISSING,
            CriterionResultStatus.MISSING,
            CriterionResultStatus.MISSING);
  }

  @Test
  void 없는_문제를_채점하면_NotFoundException이_발생한다() {
    assertThatThrownBy(() -> scoringEngine.score(999L, "답안"))
        .isInstanceOf(NotFoundException.class);
  }
}
