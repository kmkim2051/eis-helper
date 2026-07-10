package skyline.eis.eishelper.scoring.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

@Entity
@Table(name = "user_answer")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class UserAnswer {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  // 로그인 시스템이 없는 동안은 세션/익명 식별자를 담는 용도. 인증 도입 전까지 nullable.
  @Column(length = 100)
  private String userId;

  // problem 도메인은 별도 애그리거트이므로 엔티티 참조 대신 id만 보관한다.
  @Column(nullable = false)
  private Long problemId;

  @Lob
  @Column(nullable = false)
  private String answerText;

  @OneToOne(mappedBy = "userAnswer", cascade = CascadeType.ALL, orphanRemoval = true)
  private ScoringResult scoringResult;

  @CreationTimestamp
  @Column(nullable = false, updatable = false)
  private LocalDateTime submittedAt;
}
