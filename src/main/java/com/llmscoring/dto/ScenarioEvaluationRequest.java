package com.llmscoring.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class ScenarioEvaluationRequest {

    @NotEmpty
    @Valid
    private List<ChatMessage> messages;

    private String sessionId;

    private String modelName;
}
