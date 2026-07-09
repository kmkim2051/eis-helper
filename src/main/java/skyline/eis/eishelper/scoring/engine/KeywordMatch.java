package skyline.eis.eishelper.scoring.engine;

import java.util.List;
import skyline.eis.eishelper.problem.entity.CriterionImportance;
import skyline.eis.eishelper.scoring.entity.CriterionResultStatus;

// 채점 기준 하나에 대한 규칙 기반(키워드/alias) 매칭 결과.
// status는 KeywordMatcher가 규칙만으로 판정 가능한 값(MATCHED/MISSING/UNKNOWN)만 가진다.
// PARTIAL/NEGATIVE는 이후 LLM 판정 단계의 몫이라 여기서 만들지 않는다.
public record KeywordMatch(
    Long criterionId,
    String content,
    CriterionImportance importance,
    CriterionResultStatus status,
    List<String> matchedKeywords) {}
