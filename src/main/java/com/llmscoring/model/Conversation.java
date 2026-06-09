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
@Table(name = "conversations")
public class Conversation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = false, nullable = false)
    private String sessionId;

    private String scenarioName;
    private String modelName;
    private String promptVersion;
    private Integer turnCount;

    // Full message history
    @Type(JsonBinaryType.class)
    @Column(columnDefinition = "jsonb")
    private List<Map<String, String>> messages;

    // Link to scoring result
    private Long scoringResultId;

    // Overall pass/fail from scoring
    private Boolean overallPassed;

    // Flag count for quick display
    private Integer flagCount;

    private Instant createdAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = Instant.now();
        if (this.flagCount == null) this.flagCount = 0;
    }
}
