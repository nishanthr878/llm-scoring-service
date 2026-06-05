package com.llmscoring.scorer;

import com.llmscoring.dto.TraceRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class HallucinationDetector extends BaseScorer {

    private final ChatClient chatClient;

    private static final String PROMPT_TEMPLATE = """
            You are an expert evaluator detecting hallucinations in AI-generated answers.
            
            Hallucination means: the answer contains specific fabricated facts such as:
            - Names, dates, or numbers not present in the context
            - URLs or citations that don't exist in the context
            - Events or claims directly contradicted by the context
            - Invented details presented as facts
            
            General world knowledge is NOT hallucination.
            Only flag specific invented claims that contradict or go beyond the context.
            
            QUESTION:
            %s
            
            CONTEXT:
            %s
            
            ANSWER:
            %s
            
            Evaluate the degree of hallucination in the answer.
            
            Respond ONLY in this exact JSON format, nothing else:
            {
              "score": <float between 0.0 and 1.0>,
              "reasoning": "<one sentence explanation>"
            }
            
            Where 0.0 means no hallucination and 1.0 means severe hallucination.
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

            return parseResponse(response, "HallucinationDetector");
        } catch (Exception e) {
            log.error("HallucinationDetector failed", e);
            return ScorerResult.error("HallucinationDetector failed: " + e.getMessage());
        }
    }
}
