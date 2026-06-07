package com.llmscoring.scorer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class ConversationFaithfulnessScorer extends BaseScorer implements Scorer {

    private final ChatClient chatClient;

    private static final String PROMPT_TEMPLATE = """
            You are an expert evaluator for RAG (Retrieval Augmented Generation) systems.
            Your job is to evaluate whether the assistant's responses across an entire
            conversation are grounded in the provided context documents.
            
            Faithful means: claims made across all assistant turns are supported by the context.
            Not faithful means: the assistant made claims not found in or contradicted by the context.
            
            CONVERSATION:
            %s
            
            CONTEXT DOCUMENTS:
            %s
            
            Evaluate the overall faithfulness of the assistant across all turns.
            
            Respond ONLY in this exact JSON format, nothing else:
            {
              "score": <float between 0.0 and 1.0>,
              "reasoning": "<one sentence explanation>"
            }
            
            Where 1.0 means completely faithful and 0.0 means completely unfaithful.
            """;

    @Override
    public String name() {
        return "conversationFaithfulness";
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
            log.error("ConversationFaithfulnessScorer failed", e);
            return ScorerResult.error("ConversationFaithfulnessScorer failed: " + e.getMessage());
        }
    }
}
