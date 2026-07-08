package skyline.eis.eishelper.problem.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "scoring_criterion")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class ScoringCriterion {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne
  @JoinColumn(name = "rubric_id", nullable = false)
  private ScoringRubric scoringRubric;

  @Column(nullable = false, length = 255)
  private String content;

  @Column(nullable = false)
  private int score;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private CriterionImportance importance;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private CriterionGradingType gradingType;

  @Column(nullable = false)
  private int orderNo;

  @Builder.Default
  @OneToMany(mappedBy = "scoringCriterion", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<CriterionAlias> aliases = new ArrayList<>();
}
