package com.llmscoring.dto.stats;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TrendPoint {
    private String date;
    private long total;
    private long passed;
    private long failed;
    private double passRate;
}
