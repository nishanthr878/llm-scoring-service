package com.llmscoring.scorer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class ContextRetentionScorer extends BaseScorer implements Scorer {

    private final ChatClient chatClient;

    private static final String PROMPT_TEMPLATE = """
            You are an expert evaluator analyzing multi-turn AI conversations.
            Your job is to evaluate whether the assistant retained context from
            earlier turns when answering later questions.
            
            Context retention means: the assistant remembered and correctly used
            information from earlier in the conversation when relevant.
            
            Poor context retention means: the assistant forgot earlier information,
            asked for details already provided, or ignored relevant prior context.
            
            CONVERSATION:
            %s
            
            RETRIEVED CONTEXT DOCUMENTS:
            %s
            
            Evaluate how well the assistant retained and used context across turns.
            
            Respond ONLY in this exact JSON format, nothing else:
            {
              "score": <float between 0.0 and 1.0>,
              "reasoning": "<one sentence explanation>"
            }
            
            Where 1.0 means perfect context retention and 0.0 means no context retention.
            """;

    @Override
    public String name() {
        return "contextRetention";
    }

    @Override
    public ScorerResult score(Map<String, Object> context) {
        String conversation = (String) context.get("formattedConversation");
        String chunks = (String) context.get("formattedChunks");

        String prompt = PROMPT_TEMPLATE.formatted(conversation, chunks);

        try {
            String response = callWithRetry(chatClient, prompt, name());
            return parseResponse(response, name());
        } catch (Exception e) {
            log.error("ContextRetentionScorer failed", e);
            return ScorerResult.error("ContextRetentionScorer failed: " + e.getMessage());
        }
    }
}
