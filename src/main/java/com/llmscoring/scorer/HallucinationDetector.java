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
public class HallucinationDetector extends BaseScorer implements Scorer {

    private final ChatClient chatClient;

    private static final String PROMPT_TEMPLATE = """
            You are an expert evaluator detecting hallucinations in AI-generated answers.
            
            Hallucination means: the answer contains specific fabricated facts such as:
            - Names, dates, or numbers not present in the context
            - URLs or citations that don't exist in the context
            - Events or claims directly contradicted by the context
            
            General world knowledge is NOT hallucination.
            
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
            
            Where 0.0 means no hallucination and 1.0 means severe hallucination.
            """;

    @Override
    public String name() {
        return "hallucination";
    }

    @Override
    public ScorerResult score(Map<String, Object> context) {
        String question = (String) context.get("question");
        String answer = (String) context.get("answer");
        List<String> chunks = (List<String>) context.get("retrievedChunks");
        String joinedContext = String.join("\n---\n", chunks);

        String prompt = PROMPT_TEMPLATE.formatted(question, joinedContext, answer);

        try {
            String response = callWithRetry(chatClient, prompt, name());
            return parseResponse(response, name());
        } catch (Exception e) {
            log.error("HallucinationDetector failed", e);
            return ScorerResult.error("HallucinationDetector failed: " + e.getMessage());
        }
    }
}
