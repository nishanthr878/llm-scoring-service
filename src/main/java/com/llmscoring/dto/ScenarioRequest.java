package com.llmscoring.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Data
public class ScenarioRequest {

    @NotBlank(message = "Scenario name must not be blank")
    private String scenarioName;

    private String description;

    @NotEmpty(message = "Policy rules must not be empty")
    private List<String> policy;

    @NotEmpty(message = "Expected behaviors must not be empty")
    private List<String> expectedBehaviors;

    @NotEmpty(message = "Conversation must not be empty")
    @Valid
    private List<ChatMessage> messages;

    private String sessionId;
    private String modelName;
    private String promptVersion;

    public Map<String, Object> toContext() {
        String formattedConversation = messages.stream()
                .map(m -> m.getRole().toUpperCase() + ": " + m.getContent())
                .collect(Collectors.joining("\n\n"));

        Map<String, Object> context = new HashMap<>();
        context.put("scenarioName", scenarioName);
        context.put("description", description);
        context.put("policy", policy);
        context.put("expectedBehaviors", expectedBehaviors);
        context.put("messages", messages);
        context.put("formattedConversation", formattedConversation);
        context.put("sessionId", sessionId);
        context.put("modelName", modelName);
        context.put("promptVersion", promptVersion);
        context.put("turnCount", messages.size());
        context.put("latencyMs", null);
        context.put("inputTokens", null);
        context.put("outputTokens", null);
        context.put("costUsd", null);
        return context;
    }
}
