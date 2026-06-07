package com.llmscoring.dto.stats;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class OverviewStats {
    private long totalEvaluations;
    private long totalPassed;
    private long totalFailed;
    private long totalInconclusive;
    private double passRate;
    private long singleTurnCount;
    private long conversationCount;
    private long scenarioCount;
}
