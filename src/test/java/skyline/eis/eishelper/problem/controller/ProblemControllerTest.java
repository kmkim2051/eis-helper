package skyline.eis.eishelper.problem.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class ProblemControllerTest {

  @Autowired private MockMvc mockMvc;

  @Test
  void 문제_목록을_조회하면_시드_3문제가_반환된다() throws Exception {
    mockMvc
        .perform(get("/api/problems"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(3))
        .andExpect(jsonPath("$[0].subject").value("APPLICATION"))
        .andExpect(jsonPath("$[0].questionText").isNotEmpty());
  }

  @Test
  void 문제_상세를_조회하면_채점기준과_추천답안은_노출되지_않는다() throws Exception {
    mockMvc
        .perform(get("/api/problems/1"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(1))
        .andExpect(jsonPath("$.category").value("SQL_INJECTION"))
        .andExpect(jsonPath("$.totalScore").value(6))
        .andExpect(jsonPath("$.recommendedAnswer").doesNotExist())
        .andExpect(jsonPath("$.explanation").doesNotExist())
        .andExpect(jsonPath("$.scoringRubric").doesNotExist());
  }

  @Test
  void 없는_문제를_조회하면_404가_반환된다() throws Exception {
    mockMvc
        .perform(get("/api/problems/999"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.status").value(404));
  }
}
