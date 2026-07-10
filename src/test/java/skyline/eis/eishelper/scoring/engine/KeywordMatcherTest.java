package skyline.eis.eishelper.scoring.engine;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import skyline.eis.eishelper.problem.entity.CriterionAlias;
import skyline.eis.eishelper.problem.entity.CriterionGradingType;
import skyline.eis.eishelper.problem.entity.CriterionImportance;
import skyline.eis.eishelper.problem.entity.ScoringCriterion;
import skyline.eis.eishelper.scoring.entity.CriterionResultStatus;

class KeywordMatcherTest {

  private final KeywordMatcher keywordMatcher = new KeywordMatcher();

  @Test
  void alias가_답안에_포함되면_MATCHED로_판정되고_매칭된_키워드가_담긴다() {
    ScoringCriterion criterion =
        criterion(
            1L,
            "PreparedStatement 또는 바인딩 변수 사용",
            CriterionImportance.CORE,
            CriterionGradingType.KEYWORD,
            List.of("PreparedStatement", "바인딩 변수"));

    KeywordMatchResult result =
        keywordMatcher.match("대응책으로 PreparedStatement를 사용한다", List.of(criterion));

    KeywordMatch match = result.matches().get(0);
    assertThat(match.status()).isEqualTo(CriterionResultStatus.MATCHED);
    assertThat(match.matchedKeywords()).containsExactly("PreparedStatement");
  }

  @Test
  void 대소문자와_공백_차이가_있어도_전처리를_통해_매칭된다() {
    ScoringCriterion criterion =
        criterion(
            1L,
            "PreparedStatement 사용",
            CriterionImportance.CORE,
            CriterionGradingType.KEYWORD,
            List.of("prepared statement"));

    KeywordMatchResult result =
        keywordMatcher.match("Prepared   Statement 를 씁니다", List.of(criterion));

    assertThat(result.matches().get(0).status()).isEqualTo(CriterionResultStatus.MATCHED);
  }

  @Test
  void KEYWORD_기준이_매칭되지_않으면_MISSING으로_확정된다() {
    ScoringCriterion criterion =
        criterion(
            1L,
            "DB 권한 최소화",
            CriterionImportance.NORMAL,
            CriterionGradingType.KEYWORD,
            List.of("DB 권한 최소화", "최소 권한 원칙"));

    KeywordMatchResult result = keywordMatcher.match("입력값을 검증한다", List.of(criterion));

    KeywordMatch match = result.matches().get(0);
    assertThat(match.status()).isEqualTo(CriterionResultStatus.MISSING);
    assertThat(match.matchedKeywords()).isEmpty();
  }

  @Test
  void SEMANTIC_기준이_매칭되지_않으면_LLM_판정_대상인_UNKNOWN으로_둔다() {
    ScoringCriterion criterion =
        criterion(
            1L,
            "공격 원리: 검증되지 않은 입력값에 SQL 구문을 삽입",
            CriterionImportance.CORE,
            CriterionGradingType.SEMANTIC,
            List.of("SQL 구문 삽입", "쿼리 조작"));

    KeywordMatchResult result =
        keywordMatcher.match("악의적인 값을 넣어 데이터베이스를 조작한다", List.of(criterion));

    assertThat(result.matches().get(0).status()).isEqualTo(CriterionResultStatus.UNKNOWN);
  }

  @Test
  void 토큰에_조사나_어미가_붙어도_접두어_매칭으로_인정된다() {
    ScoringCriterion criterion =
        criterion(
            1L,
            "입력값 검증 또는 화이트리스트 검증",
            CriterionImportance.NORMAL,
            CriterionGradingType.KEYWORD,
            List.of("입력값 검증"));

    KeywordMatchResult result = keywordMatcher.match("입력값을 검증한다", List.of(criterion));

    assertThat(result.matches().get(0).status()).isEqualTo(CriterionResultStatus.MATCHED);
  }

  @Test
  void 답안이_alias를_붙여_써도_공백_제거_매칭으로_인정된다() {
    ScoringCriterion criterion =
        criterion(
            1L,
            "입력값 검증",
            CriterionImportance.NORMAL,
            CriterionGradingType.KEYWORD,
            List.of("입력값 검증"));

    KeywordMatchResult result = keywordMatcher.match("입력값검증이 필요하다", List.of(criterion));

    assertThat(result.matches().get(0).status()).isEqualTo(CriterionResultStatus.MATCHED);
  }

  @Test
  void 붙여_쓴_alias_하나로_띄어_쓴_답안도_커버한다() {
    ScoringCriterion criterion =
        criterion(
            1L,
            "PreparedStatement 사용",
            CriterionImportance.CORE,
            CriterionGradingType.KEYWORD,
            List.of("PreparedStatement"));

    KeywordMatchResult result =
        keywordMatcher.match("Prepared Statement를 사용해 방어한다", List.of(criterion));

    assertThat(result.matches().get(0).status()).isEqualTo(CriterionResultStatus.MATCHED);
  }

  @Test
  void 키워드_토큰_순서가_다르면_매칭되지_않는다() {
    ScoringCriterion criterion =
        criterion(
            1L,
            "입력값 검증",
            CriterionImportance.NORMAL,
            CriterionGradingType.KEYWORD,
            List.of("입력값 검증"));

    KeywordMatchResult result = keywordMatcher.match("검증 입력값", List.of(criterion));

    assertThat(result.matches().get(0).status()).isEqualTo(CriterionResultStatus.MISSING);
  }

  @Test
  void 키워드_토큰_사이에_다른_단어가_끼면_매칭되지_않는다() {
    ScoringCriterion criterion =
        criterion(
            1L,
            "CSRF 토큰 검증",
            CriterionImportance.CORE,
            CriterionGradingType.KEYWORD,
            List.of("토큰 검증"));

    KeywordMatchResult result = keywordMatcher.match("토큰 없이 검증한다", List.of(criterion));

    assertThat(result.matches().get(0).status()).isEqualTo(CriterionResultStatus.MISSING);
  }

  @Test
  void alias가_없는_기준은_content로_매칭한다() {
    ScoringCriterion criterion =
        criterion(1L, "WAF 적용", CriterionImportance.OPTIONAL, CriterionGradingType.KEYWORD, List.of());

    KeywordMatchResult result = keywordMatcher.match("WAF 적용을 고려한다", List.of(criterion));

    assertThat(result.matches().get(0).status()).isEqualTo(CriterionResultStatus.MATCHED);
  }

  @Test
  void 전처리하면_비는_키워드는_무시되어_오탐되지_않는다() {
    ScoringCriterion criterion =
        criterion(1L, "특수문자 기준", CriterionImportance.NORMAL, CriterionGradingType.KEYWORD, List.of("!!!"));

    KeywordMatchResult result = keywordMatcher.match("아무 답안", List.of(criterion));

    assertThat(result.matches().get(0).status()).isEqualTo(CriterionResultStatus.MISSING);
  }

  @Test
  void 매칭되지_않은_CORE_KEYWORD_기준은_missingCoreCriteria로_수집된다() {
    ScoringCriterion missingCore =
        criterion(1L, "출력값 인코딩", CriterionImportance.CORE, CriterionGradingType.KEYWORD, List.of("출력값 인코딩"));
    ScoringCriterion missingNormal =
        criterion(2L, "HttpOnly 설정", CriterionImportance.NORMAL, CriterionGradingType.KEYWORD, List.of("HttpOnly"));
    ScoringCriterion matchedCore =
        criterion(3L, "CSP 적용", CriterionImportance.CORE, CriterionGradingType.KEYWORD, List.of("CSP"));

    KeywordMatchResult result =
        keywordMatcher.match("CSP를 적용한다", List.of(missingCore, missingNormal, matchedCore));

    assertThat(result.hasMissingCoreCriteria()).isTrue();
    assertThat(result.missingCoreCriteria()).extracting(KeywordMatch::criterionId).containsExactly(1L);
  }

  @Test
  void 매칭되지_않은_CORE_SEMANTIC_기준은_UNKNOWN이므로_missingCore에_포함되지_않는다() {
    ScoringCriterion semanticCore =
        criterion(1L, "공격 원리 서술", CriterionImportance.CORE, CriterionGradingType.SEMANTIC, List.of("스크립트 삽입"));

    KeywordMatchResult result = keywordMatcher.match("전혀 관련 없는 답안", List.of(semanticCore));

    assertThat(result.matches().get(0).status()).isEqualTo(CriterionResultStatus.UNKNOWN);
    assertThat(result.hasMissingCoreCriteria()).isFalse();
  }

  private ScoringCriterion criterion(
      Long id,
      String content,
      CriterionImportance importance,
      CriterionGradingType gradingType,
      List<String> aliases) {
    return ScoringCriterion.builder()
        .id(id)
        .content(content)
        .score(1)
        .importance(importance)
        .gradingType(gradingType)
        .orderNo(1)
        .aliases(aliases.stream().map(alias -> CriterionAlias.builder().alias(alias).build()).toList())
        .build();
  }
}
