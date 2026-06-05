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
public class FaithfulnessScorer extends BaseScorer implements Scorer {

    private final ChatClient chatClient;

    private static final String PROMPT_TEMPLATE = """
            You are an expert evaluator for RAG (Retrieval Augmented Generation) systems.
            Your job is to evaluate whether the given answer is faithful to the provided context.
            
            Faithful means: every claim in the answer is supported by the context.
            Not faithful means: the answer contains claims not found in or contradicted by the context.
            
            QUESTION:
            %s
            
            CONTEXT:
            %s
            
            ANSWER:
            %s
            
            Respond ONLY in this exact JSON format, nothing else:
            {
              "score": <float between 0.0 and 1.0>,
              "reasoning": "<one sentence explanation>"
            }
            """;

    @Override
    public String name() {
        return "faithfulness";
    }

    @Override
    public ScorerResult score(Map<String, Object> context) {
        String question = (String) context.get("question");
        String answer = (String) context.get("answer");
        List<String> chunks = (List<String>) context.get("retrievedChunks");
        String joinedContext = String.join("\n---\n", chunks);

        String prompt = PROMPT_TEMPLATE.formatted(question, joinedContext, answer);

        try {
            String response = chatClient.prompt()
                    .user(prompt)
                    .call()
                    .content();
            return parseResponse(response, name());
        } catch (Exception e) {
            log.error("FaithfulnessScorer failed", e);
            return ScorerResult.error("FaithfulnessScorer failed: " + e.getMessage());
        }
    }
}
