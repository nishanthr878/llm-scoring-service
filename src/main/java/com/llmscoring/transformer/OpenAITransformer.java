package com.llmscoring.transformer;

import com.fasterxml.jackson.databind.JsonNode;
import com.llmscoring.dto.ChatMessage;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class OpenAITransformer implements MessageTransformer {

    @Override
    public String formatName() {
        return "openai";
    }

    @Override
    public List<ChatMessage> transform(JsonNode messagesNode) {
        List<ChatMessage> messages = new ArrayList<>();

        for (JsonNode node : messagesNode) {
            String role = node.path("role").asText();
            String content = node.path("content").asText();

            if (role.isBlank() || content.isBlank()) continue;

            // Normalize role — only accept user/assistant
            String normalizedRole = normalizeRole(role);
            if (normalizedRole == null) continue;

            ChatMessage msg = new ChatMessage();
            msg.setRole(normalizedRole);
            msg.setContent(content);
            messages.add(msg);
        }

        return messages;
    }

    private String normalizeRole(String role) {
        return switch (role.toLowerCase()) {
            case "user", "human", "customer" -> "user";
            case "assistant", "bot", "agent", "ai" -> "assistant";
            default -> null; // skip system, tool, etc.
        };
    }
}
