package com.llmscoring.dto.stats;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class FailureStats {
    private String scorerName;
    private long failureCount;
    private double avgScore;
}
