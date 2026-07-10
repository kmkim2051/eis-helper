package skyline.eis.eishelper.scoring.engine;

import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;
import skyline.eis.eishelper.problem.entity.CriterionAlias;
import skyline.eis.eishelper.problem.entity.CriterionGradingType;
import skyline.eis.eishelper.problem.entity.ScoringCriterion;
import skyline.eis.eishelper.scoring.entity.CriterionResultStatus;

// 답안을 채점 기준의 키워드/alias와 대조하는 규칙 기반 매칭기.
// 답안과 키워드를 동일한 규칙(AnswerPreprocessor)으로 정규화한 뒤 두 전략 중 하나로 판정한다:
// 1) 공백 제거 포함 — 띄어쓰기 변형("입력값검증", "prepared statement"↔"preparedstatement") 허용
// 2) 토큰 접두어 — 조사/어미가 붙은 표현("입력값을 검증한다" ⊇ "입력값 검증") 허용
@Component
public class KeywordMatcher {

  public KeywordMatchResult match(String answerText, List<ScoringCriterion> criteria) {
    String normalizedAnswer = AnswerPreprocessor.preprocess(answerText);
    List<KeywordMatch> matches = new ArrayList<>();
    for (ScoringCriterion criterion : criteria) {
      matches.add(matchCriterion(normalizedAnswer, criterion));
    }
    return new KeywordMatchResult(matches);
  }

  private KeywordMatch matchCriterion(String normalizedAnswer, ScoringCriterion criterion) {
    List<String> matchedKeywords = new ArrayList<>();
    for (String keyword : keywordsOf(criterion)) {
      String normalizedKeyword = AnswerPreprocessor.preprocess(keyword);
      // 전처리 후 빈 키워드는 건너뛴다. contains("")는 항상 true라 오탐이 되기 때문.
      if (normalizedKeyword.isEmpty()) {
        continue;
      }
      if (matches(normalizedAnswer, normalizedKeyword)) {
        matchedKeywords.add(keyword);
      }
    }
    CriterionResultStatus status = resolveStatus(criterion, !matchedKeywords.isEmpty());
    return new KeywordMatch(
        criterion.getId(),
        criterion.getContent(),
        criterion.getImportance(),
        status,
        List.copyOf(matchedKeywords));
  }

  private boolean matches(String normalizedAnswer, String normalizedKeyword) {
    if (normalizedAnswer.replace(" ", "").contains(normalizedKeyword.replace(" ", ""))) {
      return true;
    }
    return matchesTokenPrefix(normalizedAnswer.split(" "), normalizedKeyword.split(" "));
  }

  // 키워드 토큰열이 답안의 "연속된" 토큰열과 순서대로 접두어 일치하는지 본다.
  // 접두어 비교라 조사/어미("입력값을", "검증한다")를 허용하되, 연속 조건으로
  // 중간에 다른 단어가 끼는 경우("토큰 없이 검증")는 인정하지 않는다 — 부정/변형 오탐 방지.
  private boolean matchesTokenPrefix(String[] answerTokens, String[] keywordTokens) {
    for (int start = 0; start <= answerTokens.length - keywordTokens.length; start++) {
      if (allTokensMatchFrom(answerTokens, keywordTokens, start)) {
        return true;
      }
    }
    return false;
  }

  private boolean allTokensMatchFrom(String[] answerTokens, String[] keywordTokens, int start) {
    for (int i = 0; i < keywordTokens.length; i++) {
      if (!answerTokens[start + i].startsWith(keywordTokens[i])) {
        return false;
      }
    }
    return true;
  }

  // alias를 키워드 후보로 사용하고, alias가 없는 기준은 content 자체를 키워드로 쓴다.
  private List<String> keywordsOf(ScoringCriterion criterion) {
    List<String> aliases = criterion.getAliases().stream().map(CriterionAlias::getAlias).toList();
    return aliases.isEmpty() ? List.of(criterion.getContent()) : aliases;
  }

  private CriterionResultStatus resolveStatus(ScoringCriterion criterion, boolean matched) {
    if (matched) {
      return CriterionResultStatus.MATCHED;
    }
    // KEYWORD 기준은 키워드가 없으면 규칙만으로 미충족을 확정한다.
    // SEMANTIC/MANUAL 기준은 의미상 충족 가능성이 남아 LLM 판정(다음 단계)으로 넘기려 UNKNOWN으로 둔다.
    return criterion.getGradingType() == CriterionGradingType.KEYWORD
        ? CriterionResultStatus.MISSING
        : CriterionResultStatus.UNKNOWN;
  }
}
