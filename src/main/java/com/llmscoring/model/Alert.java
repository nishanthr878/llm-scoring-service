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
@Table(name = "alerts")
public class Alert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long scoringResultId;
    private Long turnFlagId;
    private String sessionId;
    private String scenarioName;
    private Integer turnIndex;

    @Enumerated(EnumType.STRING)
    private FlagType flagType;

    @Enumerated(EnumType.STRING)
    private Severity severity;

    private String scorerName;
    private Double score;
    private Double threshold;

    @Column(columnDefinition = "TEXT")
    private String reason;

    private Boolean dismissed;
    private Instant firedAt;

    @PrePersist
    public void prePersist() {
        this.firedAt = Instant.now();
        if (this.dismissed == null) this.dismissed = false;
    }
}
