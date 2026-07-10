package skyline.eis.eishelper.problem.service;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import skyline.eis.eishelper.common.exception.NotFoundException;
import skyline.eis.eishelper.problem.dto.ProblemResponse;
import skyline.eis.eishelper.problem.repository.ProblemRepository;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProblemService {

  private final ProblemRepository problemRepository;

  public List<ProblemResponse> findProblems() {
    return problemRepository.findAll().stream().map(ProblemResponse::from).toList();
  }

  public ProblemResponse findProblem(Long problemId) {
    return problemRepository
        .findById(problemId)
        .map(ProblemResponse::from)
        .orElseThrow(() -> new NotFoundException("문제를 찾을 수 없습니다: " + problemId));
  }
}
