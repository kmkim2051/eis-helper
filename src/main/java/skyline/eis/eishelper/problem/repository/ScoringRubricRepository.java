package skyline.eis.eishelper.problem.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import skyline.eis.eishelper.problem.entity.ScoringRubric;

public interface ScoringRubricRepository extends JpaRepository<ScoringRubric, Long> {

  Optional<ScoringRubric> findByProblemId(Long problemId);
}
