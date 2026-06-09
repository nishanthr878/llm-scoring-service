package com.llmscoring.model;

import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Type;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@Entity
@Table(name = "scenarios")
public class Scenario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    // Policy rules — ["Always collect order ID", "Never promise same-day refund"]
    @Type(JsonBinaryType.class)
    @Column(columnDefinition = "jsonb", nullable = false)
    private List<String> policy;

    // Expected behaviors — ["Bot collected order ID", "Bot mentioned refund timeline"]
    @Type(JsonBinaryType.class)
    @Column(columnDefinition = "jsonb", nullable = false)
    private List<String> expectedBehaviors;

    // Which scorers to run — ["policyCompliance", "expectedBehavior", "consistency"]
    @Type(JsonBinaryType.class)
    @Column(columnDefinition = "jsonb", nullable = false)
    private List<String> scorerNames;

    // Flag if any score drops below this
    private Double alertThreshold;

    // PII detection config
    @Type(JsonBinaryType.class)
    @Column(columnDefinition = "jsonb")
    private List<String> piiTypesToDetect; // ["EMAIL", "PHONE", "CREDIT_CARD"]

    private Boolean piiDetectionEnabled;

    // Custom pattern config
    @Type(JsonBinaryType.class)
    @Column(columnDefinition = "jsonb")
    private List<Map<String, String>> customPatterns;// [{"name": "competitor", "pattern": "CompetitorX|Y", "severity": "HIGH"}]

    private Boolean turnLevelScoringEnabled;

    private Boolean active;

    private Instant createdAt;
    private Instant updatedAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
        if (this.active == null) this.active = true;
        if (this.alertThreshold == null) this.alertThreshold = 0.7;
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = Instant.now();
    }
}
