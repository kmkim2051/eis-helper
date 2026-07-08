package skyline.eis.eishelper.problem.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "problem")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class Problem {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 30)
  private Subject subject;

  @Column(nullable = false, length = 50)
  private String category;

  @Lob
  @Column(nullable = false)
  private String questionText;

  @Column(nullable = false)
  private int totalScore;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private AnswerType answerType;

  @Lob
  private String explanation;

  @Lob
  private String recommendedAnswer;

  // 연관관계 소유자는 ScoringRubric (problem_id FK). 여기서는 조회 편의용 읽기 전용 참조.
  @OneToOne(mappedBy = "problem", cascade = CascadeType.ALL, orphanRemoval = true)
  private ScoringRubric scoringRubric;

  @CreationTimestamp
  @Column(nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @UpdateTimestamp
  @Column(nullable = false)
  private LocalDateTime updatedAt;
}
