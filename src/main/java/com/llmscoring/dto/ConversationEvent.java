package com.llmscoring.dto;

import lombok.Data;

import java.time.Instant;
import java.util.List;

@Data
public class ConversationEvent {

    private String eventId;
    private String sessionId;
    private String scenarioName;
    private String modelName;
    private List<ChatMessage> messages;
    private List<String> retrievedChunks;
    private Instant timestamp;
}
