package com.llmscoring.transformer;

import com.fasterxml.jackson.databind.JsonNode;
import com.llmscoring.dto.ChatMessage;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class HumanAITransformer implements MessageTransformer {

    @Override
    public String formatName() {
        return "human-ai";
    }

    @Override
    public List<ChatMessage> transform(JsonNode messagesNode) {
        List<ChatMessage> messages = new ArrayList<>();

        for (JsonNode node : messagesNode) {
            String speaker = node.path("speaker").asText();
            String text = node.path("text").asText();

            if (speaker.isBlank() || text.isBlank()) continue;

            String role = speaker.equalsIgnoreCase("human") ? "user" : "assistant";

            ChatMessage msg = new ChatMessage();
            msg.setRole(role);
            msg.setContent(text);
            messages.add(msg);
        }

        return messages;
    }
}
