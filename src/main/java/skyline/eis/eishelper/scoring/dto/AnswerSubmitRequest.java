package skyline.eis.eishelper.scoring.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AnswerSubmitRequest(
    @NotNull(message = "problemId는 필수입니다") Long problemId,
    @NotBlank(message = "answerText는 비어 있을 수 없습니다") String answerText) {}
