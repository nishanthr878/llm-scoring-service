package com.llmscoring.scorer;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ScorerResult {

    private double score;
    private String reasoning;
    private boolean success;

    public static ScorerResult error(String reason) {
        return new ScorerResult(0.0, reason, false);
    }
}
