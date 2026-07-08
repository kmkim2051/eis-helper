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
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "scoring_rubric")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class ScoringRubric {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @OneToOne
  @JoinColumn(name = "problem_id", nullable = false, unique = true)
  private Problem problem;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private RubricGradingPolicy gradingPolicy;

  @Column(length = 255)
  private String description;

  @Builder.Default
  @OneToMany(mappedBy = "scoringRubric", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<ScoringCriterion> criteria = new ArrayList<>();
}
