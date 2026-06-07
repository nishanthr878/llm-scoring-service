package com.llmscoring.dto.stats;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
public class ScenarioStats {
    private String scenarioName;
    private long totalEvaluations;
    private long passed;
    private long failed;
    private double passRate;
    private Map<String, Double> avgScores;
}
