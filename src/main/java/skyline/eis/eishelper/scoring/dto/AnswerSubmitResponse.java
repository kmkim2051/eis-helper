package skyline.eis.eishelper.scoring.dto;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import skyline.eis.eishelper.scoring.engine.AnswerPreprocessor;
import skyline.eis.eishelper.scoring.engine.KeywordMatch;
import skyline.eis.eishelper.scoring.engine.ScoringOutcome;
import skyline.eis.eishelper.scoring.entity.CriterionResultStatus;

// score는 실제 시험 점수가 아닌 "채점 기준 기반 예상 점수"다 (engineering-guidelines 표현 원칙).
// matchedKeywords는 답안에서 실제 인정된 키워드(alias), partial/missingKeywords는 기준 내용(content) —
// 누락 안내는 어떤 기준을 못 채웠는지가 중요하므로 개별 키워드가 아닌 기준 단위로 알려준다.
public record AnswerSubmitResponse(
    Long problemId,
    int score,
    int totalScore,
    List<String> matchedKeywords,
    List<String> partialKeywords,
    List<String> missingKeywords) {

  public static AnswerSubmitResponse from(ScoringOutcome outcome) {
    List<KeywordMatch> matches = outcome.matchResult().matches();
    return new AnswerSubmitResponse(
        outcome.scoringResult().getUserAnswer().getProblemId(),
        outcome.scoringResult().getScore(),
        outcome.scoringResult().getTotalScore(),
        deduplicate(
            matches.stream()
                .filter(match -> match.status() == CriterionResultStatus.MATCHED)
                .flatMap(match -> match.matchedKeywords().stream())
                .toList()),
        contentsOf(matches, CriterionResultStatus.PARTIAL),
        // UNKNOWN(규칙으로 판정 불가)은 사용자에게 missing으로 합쳐 노출한다.
        // DB에는 UNKNOWN으로 구분 저장되어 Phase 2 LLM 재판정 대상으로 식별 가능.
        matches.stream()
            .filter(
                match ->
                    match.status() == CriterionResultStatus.MISSING
                        || match.status() == CriterionResultStatus.UNKNOWN)
            .map(KeywordMatch::content)
            .toList());
  }

  // "PreparedStatement"와 "prepared statement"처럼 표기만 다른 alias가 동시에 매칭될 수 있어,
  // 정규화(공백 제거) 기준으로 같은 키워드는 첫 표기 하나만 노출한다.
  private static List<String> deduplicate(List<String> keywords) {
    Map<String, String> uniqueByNormalizedForm = new LinkedHashMap<>();
    for (String keyword : keywords) {
      uniqueByNormalizedForm.putIfAbsent(
          AnswerPreprocessor.preprocess(keyword).replace(" ", ""), keyword);
    }
    return List.copyOf(uniqueByNormalizedForm.values());
  }

  private static List<String> contentsOf(List<KeywordMatch> matches, CriterionResultStatus status) {
    return matches.stream()
        .filter(match -> match.status() == status)
        .map(KeywordMatch::content)
        .toList();
  }
}
