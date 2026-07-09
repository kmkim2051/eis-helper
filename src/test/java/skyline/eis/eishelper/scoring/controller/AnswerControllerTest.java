package skyline.eis.eishelper.scoring.controller;

import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

// 시드 문제 1(SQL Injection, 6점) 기반. 채점 로직 자체는 ScoringEngineTest가 검증하므로
// 여기서는 API 계약(요청/응답 형태, 상태코드, 검증)에 집중한다.
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AnswerControllerTest {

  @Autowired private MockMvc mockMvc;

  @Test
  void 답안을_제출하면_예상_점수와_인정_누락_키워드가_반환된다() throws Exception {
    mockMvc
        .perform(
            post("/api/answers/submit")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "problemId": 1,
                      "answerText": "입력값에 SQL 구문을 삽입하는 공격이다. PreparedStatement를 사용하고 입력값을 검증한다."
                    }
                    """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.problemId").value(1))
        .andExpect(jsonPath("$.score").value(5))
        .andExpect(jsonPath("$.totalScore").value(6))
        // "prepared statement" alias도 함께 매칭되지만 정규화 중복 제거로 첫 표기만 노출된다
        .andExpect(jsonPath("$.matchedKeywords.length()").value(3))
        .andExpect(jsonPath("$.matchedKeywords", hasItem("PreparedStatement")))
        .andExpect(jsonPath("$.matchedKeywords", hasItem("입력값 검증")))
        .andExpect(jsonPath("$.partialKeywords").isEmpty())
        .andExpect(jsonPath("$.missingKeywords.length()").value(1))
        .andExpect(jsonPath("$.missingKeywords", hasItem("DB 권한 최소화 또는 에러 메시지 노출 제한")));
  }

  @Test
  void 아무_기준도_충족하지_못하면_0점이고_판정불가_기준도_missing으로_노출된다() throws Exception {
    mockMvc
        .perform(
            post("/api/answers/submit")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"problemId": 1, "answerText": "정보보안기사 자격증 공부"}
                    """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.score").value(0))
        .andExpect(jsonPath("$.matchedKeywords").isEmpty())
        .andExpect(jsonPath("$.missingKeywords.length()").value(4));
  }

  @Test
  void answerText가_비어있으면_400이_반환된다() throws Exception {
    mockMvc
        .perform(
            post("/api/answers/submit")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"problemId": 1, "answerText": "   "}
                    """))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value(400));
  }

  @Test
  void problemId가_없으면_400이_반환된다() throws Exception {
    mockMvc
        .perform(
            post("/api/answers/submit")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"answerText": "PreparedStatement를 사용한다"}
                    """))
        .andExpect(status().isBadRequest());
  }

  @Test
  void 없는_문제에_제출하면_404가_반환된다() throws Exception {
    mockMvc
        .perform(
            post("/api/answers/submit")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"problemId": 999, "answerText": "PreparedStatement를 사용한다"}
                    """))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.status").value(404));
  }
}
