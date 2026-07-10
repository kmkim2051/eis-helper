package skyline.eis.eishelper.scoring.engine;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class AnswerPreprocessorTest {

  @Test
  void 연속된_공백과_탭_줄바꿈은_하나의_공백으로_정리된다() {
    String result = AnswerPreprocessor.preprocess("SQL   Injection\t공격\n원리");

    assertThat(result).isEqualTo("sql injection 공격 원리");
  }

  @Test
  void 앞뒤_공백은_제거된다() {
    String result = AnswerPreprocessor.preprocess("   PreparedStatement 사용   ");

    assertThat(result).isEqualTo("preparedstatement 사용");
  }

  @Test
  void 영문_대소문자는_소문자로_정규화된다() {
    String result = AnswerPreprocessor.preprocess("PreparedStatement");

    assertThat(result).isEqualTo(AnswerPreprocessor.preprocess("preparedstatement"));
  }

  @Test
  void 특수문자와_문장부호는_공백으로_치환된다() {
    String result = AnswerPreprocessor.preprocess("SQL Injection(공격)!! 대응 방안은? #보안");

    assertThat(result).isEqualTo("sql injection 공격 대응 방안은 보안");
  }

  @Test
  void 한글과_영문_숫자는_보존된다() {
    String result = AnswerPreprocessor.preprocess("XSS는 CVE-2021-1234와 관련이 있다");

    assertThat(result).isEqualTo("xss는 cve 2021 1234와 관련이 있다");
  }

  @Test
  void null_입력은_빈_문자열로_처리된다() {
    assertThat(AnswerPreprocessor.preprocess(null)).isEmpty();
  }

  @Test
  void 빈_문자열이나_공백만_있는_입력은_빈_문자열이_된다() {
    assertThat(AnswerPreprocessor.preprocess("")).isEmpty();
    assertThat(AnswerPreprocessor.preprocess("   ")).isEmpty();
  }
}
