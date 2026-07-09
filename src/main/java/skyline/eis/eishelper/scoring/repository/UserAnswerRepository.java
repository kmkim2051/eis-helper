package skyline.eis.eishelper.scoring.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import skyline.eis.eishelper.scoring.entity.UserAnswer;

public interface UserAnswerRepository extends JpaRepository<UserAnswer, Long> {}
