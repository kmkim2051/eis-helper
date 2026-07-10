package skyline.eis.eishelper.problem.controller;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import skyline.eis.eishelper.problem.dto.ProblemResponse;
import skyline.eis.eishelper.problem.service.ProblemService;

@RestController
@RequestMapping("/api/problems")
@RequiredArgsConstructor
public class ProblemController {

  private final ProblemService problemService;

  @GetMapping
  public List<ProblemResponse> getProblems() {
    return problemService.findProblems();
  }

  @GetMapping("/{problemId}")
  public ProblemResponse getProblem(@PathVariable Long problemId) {
    return problemService.findProblem(problemId);
  }
}
