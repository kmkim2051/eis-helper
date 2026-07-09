package skyline.eis.eishelper.scoring.engine;

import java.util.List;
import skyline.eis.eishelper.problem.entity.CriterionImportance;
import skyline.eis.eishelper.scoring.entity.CriterionResultStatus;

// 답안 하나에 대한 전체 채점 기준의 규칙 기반 매칭 결과 묶음.
public record KeywordMatchResult(List<KeywordMatch> matches) {

  // 규칙만으로 미충족이 확정된 CORE 기준. gradingType이 SEMANTIC인 CORE 기준은
  // 키워드 미출현이더라도 의미상 충족 가능성이 남아 UNKNOWN이므로 여기 포함되지 않는다.
  public List<KeywordMatch> missingCoreCriteria() {
    return matches.stream()
        .filter(match -> match.importance() == CriterionImportance.CORE)
        .filter(match -> match.status() == CriterionResultStatus.MISSING)
        .toList();
  }

  public boolean hasMissingCoreCriteria() {
    return !missingCoreCriteria().isEmpty();
  }
}
