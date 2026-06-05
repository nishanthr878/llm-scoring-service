package com.llmscoring.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class TraceRequest {

    @NotBlank(message = "Question must not be blank")
    private String question;

    @NotEmpty(message = "At least one context chunk is required")
    private List<String> retrievedChunks;

    @NotBlank(message = "Answer must not be blank")
    private String answer;

    private String sessionId;
    private String modelName;
    private String promptVersion;
    private Long latencyMs;
    private Integer inputTokens;
    private Integer outputTokens;
    private Double costUsd;
}
