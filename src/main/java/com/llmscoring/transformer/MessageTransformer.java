package com.llmscoring.transformer;

import com.fasterxml.jackson.databind.JsonNode;
import com.llmscoring.dto.ChatMessage;

import java.util.List;

public interface MessageTransformer {

    // Unique format name — used in ingest request as "format" field
    String formatName();

    // Transform raw JSON messages into our ChatMessage format
    List<ChatMessage> transform(JsonNode messagesNode);

    // Validate the raw format before transforming
    default boolean supports(JsonNode messagesNode) {
        return messagesNode != null && messagesNode.isArray();
    }
}
