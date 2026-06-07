package com.llmscoring.scorer;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@AllArgsConstructor
public class ScorerResult {

    private double score;
    private String reasoning;
    private boolean success;

    // For structured results like per-behavior scores
    private List<Map<String, Object>> details;

    public ScorerResult(double score, String reasoning, boolean success) {
        this.score = score;
        this.reasoning = reasoning;
        this.success = success;
        this.details = null;
    }

    public static ScorerResult error(String reason) {
        return new ScorerResult(0.0, reason, false);
    }

    public static ScorerResult withDetails(
            double score,
            String reasoning,
            List<Map<String, Object>> details) {
        return new ScorerResult(score, reasoning, true, details);
    }
}
