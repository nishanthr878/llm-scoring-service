package com.llmscoring.model;

import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Type;

import java.time.Instant;
import java.util.Map;

import com.llmscoring.enums.ScoringType;

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

    @Enumerated(EnumType.STRING)
    private ScoringType type;

    // All scores stored as JSONB — flexible, no hardcoded fields
    @Type(JsonBinaryType.class)
    @Column(columnDefinition = "jsonb")
    private Map<String, Double> scores;

    // All reasoning stored as JSONB
    @Type(JsonBinaryType.class)
    @Column(columnDefinition = "jsonb")
    private Map<String, String> reasoning;

    // All pass/fail stored as JSONB
    @Type(JsonBinaryType.class)
    @Column(columnDefinition = "jsonb")
    private Map<String, Boolean> passed;

    private Boolean overallPassed;

    @Column(columnDefinition = "TEXT")
    private String flagReasons;

    // System metrics
    private Long latencyMs;
    private Integer inputTokens;
    private Integer outputTokens;
    private Double costUsd;
    private Integer turnCount;

    private Instant scoredAt;

    @PrePersist
    public void prePersist() {
        this.scoredAt = Instant.now();
    }
}
