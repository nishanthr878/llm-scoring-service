package com.llmscoring.transformer;

import com.fasterxml.jackson.databind.JsonNode;
import com.llmscoring.dto.ChatMessage;
import com.llmscoring.dto.FieldMapping;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class FieldMappingTransformer implements MessageTransformer {

    // Default mapping — overridden per request
    private FieldMapping mapping = new FieldMapping();

    @Override
    public String formatName() {
        return "field-mapping";
    }

    @Override
    public List<ChatMessage> transform(JsonNode messagesNode) {
        return transform(messagesNode, mapping);
    }

    public List<ChatMessage> transform(JsonNode messagesNode, FieldMapping mapping) {
        List<ChatMessage> messages = new ArrayList<>();

        for (JsonNode node : messagesNode) {
            String roleValue = node.path(mapping.getRoleField()).asText();
            String content = node.path(mapping.getContentField()).asText();

            if (roleValue.isBlank() || content.isBlank()) continue;

            String role = roleValue.equalsIgnoreCase(mapping.getUserRoleValue())
                    ? "user" : "assistant";

            ChatMessage msg = new ChatMessage();
            msg.setRole(role);
            msg.setContent(content);
            messages.add(msg);
        }

        return messages;
    }
}
