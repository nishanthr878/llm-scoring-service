package com.llmscoring.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    public Map<String, Object> toContext() {
        Map<String, Object> context = new HashMap<>();
        context.put("question", question);
        context.put("answer", answer);
        context.put("retrievedChunks", retrievedChunks);
        context.put("sessionId", sessionId);
        context.put("modelName", modelName);
        context.put("promptVersion", promptVersion);
        context.put("latencyMs", latencyMs);
        context.put("inputTokens", inputTokens);
        context.put("outputTokens", outputTokens);
        context.put("costUsd", costUsd);
        context.put("turnCount", 1);
        return context;
    }
}
