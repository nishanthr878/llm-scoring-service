package com.llmscoring.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import lombok.Data;

@Data
@Configuration
@ConfigurationProperties(prefix = "scoring")
public class ScoringConfig {

    private Thresholds thresholds = new Thresholds();
    private Flags flags = new Flags();

    @Data
    public static class Thresholds {
        private double faithfulness = 0.7;
        private double answerRelevance = 0.7;
        private double hallucination = 0.5;
    }

    @Data
    public static class Flags {
        private double costSpikeMultiplier = 3.0;
    }
}
