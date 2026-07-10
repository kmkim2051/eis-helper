package skyline.eis.eishelper.scoring.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import skyline.eis.eishelper.scoring.dto.AnswerSubmitRequest;
import skyline.eis.eishelper.scoring.dto.AnswerSubmitResponse;
import skyline.eis.eishelper.scoring.engine.ScoringEngine;
import skyline.eis.eishelper.scoring.engine.ScoringOutcome;

// 트랜잭션은 ScoringEngine.score()가 소유한다. 여기서는 요청/응답 변환만 담당.
@Service
@RequiredArgsConstructor
public class AnswerService {

  private final ScoringEngine scoringEngine;

  public AnswerSubmitResponse submit(AnswerSubmitRequest request) {
    ScoringOutcome outcome = scoringEngine.score(request.problemId(), request.answerText());
    return AnswerSubmitResponse.from(outcome);
  }
}
