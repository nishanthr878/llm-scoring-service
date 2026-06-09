package com.llmscoring.model;

import com.llmscoring.enums.FlagType;
import com.llmscoring.enums.Severity;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@Entity
@Table(name = "turn_flags")
public class TurnFlag {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long scoringResultId;
    private String sessionId;
    private String scenarioName;

    // Which turn triggered this flag (0-indexed)
    private Integer turnIndex;

    // The actual message content that triggered it
    @Column(columnDefinition = "TEXT")
    private String turnContent;

    // Who said it
    private String role; // "user" or "assistant"

    @Enumerated(EnumType.STRING)
    private FlagType flagType;

    @Enumerated(EnumType.STRING)
    private Severity severity;

    private String scorerName;
    private Double score;

    @Column(columnDefinition = "TEXT")
    private String description;

    // For PII — masked value, never raw
    private String maskedValue;

    // For custom patterns
    private String patternName;

    private Boolean dismissed;
    private Instant flaggedAt;

    @PrePersist
    public void prePersist() {
        this.flaggedAt = Instant.now();
        if (this.dismissed == null) this.dismissed = false;
        if (this.severity == null) this.severity = Severity.MEDIUM;
    }
}
