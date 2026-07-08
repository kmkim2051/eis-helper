package skyline.eis.eishelper.problem.seed;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import skyline.eis.eishelper.problem.entity.CriterionAlias;
import skyline.eis.eishelper.problem.entity.Problem;
import skyline.eis.eishelper.problem.entity.ScoringCriterion;
import skyline.eis.eishelper.problem.entity.ScoringRubric;
import skyline.eis.eishelper.problem.repository.ProblemRepository;
import skyline.eis.eishelper.problem.repository.ScoringRubricRepository;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

/**
 * data/seed/problems.json을 읽어 problem 애그리거트를 채운다. DB가 비어 있을 때만 동작한다.
 */
@Component
@RequiredArgsConstructor
public class ProblemSeeder implements CommandLineRunner {

  private static final String SEED_FILE = "data/seed/problems.json";

  private final ProblemRepository problemRepository;
  private final ScoringRubricRepository scoringRubricRepository;
  private final ObjectMapper objectMapper;

  @Override
  public void run(String... args) throws IOException {
    if (problemRepository.count() > 0) {
      return;
    }
    readSeeds().forEach(this::save);
  }

  private List<ProblemSeed> readSeeds() throws IOException {
    ClassPathResource resource = new ClassPathResource(SEED_FILE);
    try (InputStream inputStream = resource.getInputStream()) {
      return objectMapper.readValue(inputStream, new TypeReference<List<ProblemSeed>>() {});
    }
  }

  private void save(ProblemSeed seed) {
    validateTotalScore(seed);

    Problem problem =
        problemRepository.save(
            Problem.builder()
                .subject(seed.subject())
                .category(seed.category())
                .questionText(seed.questionText())
                .totalScore(seed.totalScore())
                .answerType(seed.answerType())
                .explanation(seed.explanation())
                .recommendedAnswer(seed.recommendedAnswer())
                .build());

    ScoringRubric rubric =
        ScoringRubric.builder()
            .problem(problem)
            .gradingPolicy(seed.rubric().gradingPolicy())
            .description(seed.rubric().description())
            .build();

    seed.rubric().criteria().forEach(criterionSeed -> rubric.getCriteria().add(toCriterion(rubric, criterionSeed)));

    scoringRubricRepository.save(rubric);
  }

  private ScoringCriterion toCriterion(ScoringRubric rubric, CriterionSeed seed) {
    ScoringCriterion criterion =
        ScoringCriterion.builder()
            .scoringRubric(rubric)
            .content(seed.content())
            .score(seed.score())
            .importance(seed.importance())
            .gradingType(seed.gradingType())
            .orderNo(seed.orderNo())
            .build();

    seed.aliases()
        .forEach(
            alias ->
                criterion
                    .getAliases()
                    .add(CriterionAlias.builder().scoringCriterion(criterion).alias(alias).build()));

    return criterion;
  }

  // problem.totalScore와 하위 criteria.score 합이 어긋나면 시드 데이터 자체가 잘못된 것이므로 기동 시점에 걸러낸다.
  private void validateTotalScore(ProblemSeed seed) {
    int criteriaScoreSum = seed.rubric().criteria().stream().mapToInt(CriterionSeed::score).sum();
    if (criteriaScoreSum != seed.totalScore()) {
      throw new IllegalStateException(
          "criteria score 합(%d)이 totalScore(%d)와 일치하지 않습니다: %s"
              .formatted(criteriaScoreSum, seed.totalScore(), seed.questionText()));
    }
  }
}
