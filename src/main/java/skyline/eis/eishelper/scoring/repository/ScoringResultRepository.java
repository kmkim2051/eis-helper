package skyline.eis.eishelper.scoring.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import skyline.eis.eishelper.scoring.entity.ScoringResult;

public interface ScoringResultRepository extends JpaRepository<ScoringResult, Long> {}
