package com.llmscoring.scorer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class ConsistencyScorer extends BaseScorer implements Scorer {

    private final ChatClient chatClient;

    private static final String PROMPT_TEMPLATE = """
            You are an expert evaluator analyzing multi-turn AI conversations.
            Your job is to evaluate whether the assistant's responses are consistent
            across the entire conversation.
            
            Inconsistency means: the assistant contradicted itself between turns,
            gave different answers to the same question, or changed facts mid-conversation.
            
            CONVERSATION:
            %s
            
            Evaluate the consistency of the assistant's responses across all turns.
            
            Respond ONLY in this exact JSON format, nothing else:
            {
              "score": <float between 0.0 and 1.0>,
              "reasoning": "<one sentence explanation>"
            }
            
            Where 1.0 means perfectly consistent and 0.0 means completely inconsistent.
            """;

    @Override
    public String name() {
        return "consistency";
    }

    @Override
    public ScorerResult score(Map<String, Object> context) {
        String conversation = (String) context.get("formattedConversation");

        String prompt = PROMPT_TEMPLATE.formatted(conversation);

        try {
            String response = callWithRetry(chatClient, prompt, name());
            return parseResponse(response, name());
        } catch (Exception e) {
            log.error("ConsistencyScorer failed", e);
            return ScorerResult.error("ConsistencyScorer failed: " + e.getMessage());
        }
    }
}
