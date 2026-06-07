package com.llmscoring.util;

import com.llmscoring.dto.ChatMessage;
import com.llmscoring.dto.ConversationRequest;

import java.util.ArrayList;
import java.util.List;

public class ChatLogParser {

    /**
     * Parses a raw chat log into a ConversationRequest.
     *
     * Expected format:
     * user: What is the capital of France?
     * assistant: The capital of France is Paris.
     * user: What is the population?
     * assistant: About 2.1 million.
     */
    public static ConversationRequest parse(
            String rawLog,
            List<String> retrievedChunks,
            String sessionId,
            String modelName,
            String promptVersion
    ) {
        List<ChatMessage> messages = new ArrayList<>();

        String[] lines = rawLog.strip().split("\n");

        for (String line : lines) {
            line = line.strip();

            if (line.isEmpty()) continue;

            if (line.toLowerCase().startsWith("user:")) {
                ChatMessage msg = new ChatMessage();
                msg.setRole("user");
                msg.setContent(line.substring(5).strip());
                messages.add(msg);

            } else if (line.toLowerCase().startsWith("assistant:")) {
                ChatMessage msg = new ChatMessage();
                msg.setRole("assistant");
                msg.setContent(line.substring(10).strip());
                messages.add(msg);

            } else {
                // Line has no role prefix — append to last message
                if (!messages.isEmpty()) {
                    ChatMessage last = messages.get(messages.size() - 1);
                    last.setContent(last.getContent() + " " + line);
                }
            }
        }

        ConversationRequest request = new ConversationRequest();
        request.setMessages(messages);
        request.setRetrievedChunks(retrievedChunks);
        request.setSessionId(sessionId);
        request.setModelName(modelName);
        request.setPromptVersion(promptVersion);
        return request;
    }
}
