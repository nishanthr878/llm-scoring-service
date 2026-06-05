package com.llmscoring.scorer;

import com.llmscoring.dto.TraceRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class FaithfulnessScorer extends BaseScorer {

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
            
            Evaluate the faithfulness of the answer to the context.
            
            Respond ONLY in this exact JSON format, nothing else:
            {
              "score": <float between 0.0 and 1.0>,
              "reasoning": "<one sentence explanation>"
            }
            
            Where 1.0 means completely faithful and 0.0 means completely unfaithful.
            """;

    public ScorerResult score(TraceRequest request) {
        String context = String.join("\n---\n", request.getRetrievedChunks());
        String prompt = PROMPT_TEMPLATE.formatted(
                request.getQuestion(),
                context,
                request.getAnswer()
        );

        try {
            String response = chatClient.prompt()
                    .user(prompt)
                    .call()
                    .content();

            return parseResponse(response, "FaithfulnessScorer");
        } catch (Exception e) {
            log.error("FaithfulnessScorer failed", e);
            return ScorerResult.error("FaithfulnessScorer failed: " + e.getMessage());
        }
    }
}
