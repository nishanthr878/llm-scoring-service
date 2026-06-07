package com.llmscoring.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.stream.Collectors;

@Data
public class ConversationRequest {

    @NotEmpty(message = "Conversation must have at least one message")
    @Size(min = 2, message = "Conversation must have at least one user and one assistant turn")
    @Valid
    private List<ChatMessage> messages;

    @NotEmpty(message = "At least one context chunk is required")
    private List<String> retrievedChunks;

    private String sessionId;
    private String modelName;
    private String promptVersion;

    public Map<String, Object> toContext() {
        // Format conversation as readable string for LLM
        String formattedConversation = messages.stream()
                .map(m -> m.getRole().toUpperCase() + ": " + m.getContent())
                .collect(Collectors.joining("\n\n"));

        // Format chunks
        String formattedChunks = String.join("\n---\n", retrievedChunks);

        Map<String, Object> context = new HashMap<>();
        context.put("messages", messages);
        context.put("formattedConversation", formattedConversation);
        context.put("formattedChunks", formattedChunks);
        context.put("retrievedChunks", retrievedChunks);
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
