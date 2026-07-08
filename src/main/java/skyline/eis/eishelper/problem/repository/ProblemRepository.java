package skyline.eis.eishelper.problem.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import skyline.eis.eishelper.problem.entity.Problem;

public interface ProblemRepository extends JpaRepository<Problem, Long> {
}
