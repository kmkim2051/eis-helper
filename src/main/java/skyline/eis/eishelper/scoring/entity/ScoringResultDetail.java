package skyline.eis.eishelper.scoring.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "scoring_result_detail")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class ScoringResultDetail {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne
  @JoinColumn(name = "scoring_result_id", nullable = false)
  private ScoringResult scoringResult;

  // problem 도메인(ScoringCriterion)은 별도 애그리거트이므로 엔티티 참조 대신 id만 보관한다.
  @Column(nullable = false)
  private Long criterionId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private CriterionResultStatus status;

  @Column(nullable = false)
  private int awardedScore;

  @Lob
  private String reason;

  // Rule 기반으로만 판정된 항목은 null. LLM(SEMANTIC) 판정 항목만 값을 가진다.
  private Double confidence;
}
