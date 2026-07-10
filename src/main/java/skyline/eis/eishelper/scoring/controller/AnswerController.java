package skyline.eis.eishelper.scoring.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import skyline.eis.eishelper.scoring.dto.AnswerSubmitRequest;
import skyline.eis.eishelper.scoring.dto.AnswerSubmitResponse;
import skyline.eis.eishelper.scoring.service.AnswerService;

@RestController
@RequestMapping("/api/answers")
@RequiredArgsConstructor
public class AnswerController {

  private final AnswerService answerService;

  // 리소스 생성(UserAnswer 저장)이 일어나지만 이 API의 의미는 "채점 수행과 결과 반환"이라 200으로 응답한다
  // (system-design API 초안과 동일). 저장된 답안 조회는 추후 GET /api/answers/history가 담당.
  @PostMapping("/submit")
  public AnswerSubmitResponse submit(@Valid @RequestBody AnswerSubmitRequest request) {
    return answerService.submit(request);
  }
}
