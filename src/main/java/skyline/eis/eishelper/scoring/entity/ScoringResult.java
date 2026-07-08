package skyline.eis.eishelper.scoring.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "scoring_result")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class ScoringResult {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @OneToOne
  @JoinColumn(name = "user_answer_id", nullable = false, unique = true)
  private UserAnswer userAnswer;

  @Column(nullable = false)
  private int score;

  // Problem.totalScore와 지금은 같은 값이지만, 채점 시점의 배점을 남기는 스냅샷이라 중복이 아니다.
  // 이후 관리자가 Problem 배점을 바꿔도 과거 채점 결과의 totalScore는 채점 당시 값 그대로 유지되어야 한다.
  @Column(nullable = false)
  private int totalScore;

  @Lob
  private String summary;

  @Builder.Default
  @OneToMany(mappedBy = "scoringResult", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<ScoringResultDetail> details = new ArrayList<>();

  @CreationTimestamp
  @Column(nullable = false, updatable = false)
  private LocalDateTime createdAt;
}
