package com.llmscoring.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@NoArgsConstructor
@Entity
@Table(name = "scoring_results")
public class ScoringResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String sessionId;
    private String modelName;
    private String promptVersion;

    @Column(columnDefinition = "TEXT")
    private String question;

    @Column(columnDefinition = "TEXT")
    private String answer;

    // Scores (0.0 to 1.0)
    private Double faithfulnessScore;
    private Double answerRelevanceScore;
    private Double hallucinationScore;

    // Reasoning from LLM
    @Column(columnDefinition = "TEXT")
    private String faithfulnessReasoning;

    @Column(columnDefinition = "TEXT")
    private String answerRelevanceReasoning;

    @Column(columnDefinition = "TEXT")
    private String hallucinationReasoning;

    // Pass/Fail flags
    private Boolean faithfulnessPassed;
    private Boolean answerRelevancePassed;
    private Boolean hallucinationPassed;
    private Boolean overallPassed;

    // System metrics
    private Long latencyMs;
    private Integer inputTokens;
    private Integer outputTokens;
    private Double costUsd;

    // Flag reasons if something failed
    @Column(columnDefinition = "TEXT")
    private String flagReasons;

    private Instant scoredAt;

    @PrePersist
    public void prePersist() {
        this.scoredAt = Instant.now();
    }
}
