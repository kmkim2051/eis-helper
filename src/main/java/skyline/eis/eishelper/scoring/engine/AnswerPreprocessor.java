package skyline.eis.eishelper.scoring.engine;

import java.util.regex.Pattern;

// 한글/영문 혼용 용어 정리, 조사 제거, 용어 표준화는 alias(CriterionAlias) 매칭 단계(KeywordMatcher)에서 처리한다.
public final class AnswerPreprocessor {

  private static final Pattern NON_ALLOWED_CHARACTER = Pattern.compile("[^0-9a-zA-Z가-힣\\s]");
  private static final Pattern WHITESPACE = Pattern.compile("\\s+");

  private AnswerPreprocessor() {}

  public static String preprocess(String answerText) {
    if (answerText == null) {
      return "";
    }

    String withoutSpecialCharacters = NON_ALLOWED_CHARACTER.matcher(answerText).replaceAll(" ");
    String withNormalizedWhitespace =
        WHITESPACE.matcher(withoutSpecialCharacters).replaceAll(" ").trim();
    return withNormalizedWhitespace.toLowerCase();
  }
}
