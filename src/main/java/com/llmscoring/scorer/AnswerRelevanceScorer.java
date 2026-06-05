package com.llmscoring.scorer;

import com.llmscoring.dto.TraceRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AnswerRelevanceScorer extends BaseScorer {

    private final ChatClient chatClient;

    private static final String PROMPT_TEMPLATE = """
            You are an expert evaluator for question-answering systems.
            Your job is to evaluate whether the given answer is relevant to the question asked.
            
            Relevant means: the answer directly addresses what was asked.
            Not relevant means: the answer is off-topic, incomplete, or addresses a different question.
            
            QUESTION:
            %s
            
            ANSWER:
            %s
            
            Evaluate how well the answer addresses the question.
            Ignore whether the answer is factually correct — only judge relevance.
            
            Respond ONLY in this exact JSON format, nothing else:
            {
              "score": <float between 0.0 and 1.0>,
              "reasoning": "<one sentence explanation>"
            }
            
            Where 1.0 means perfectly relevant and 0.0 means completely irrelevant.
            """;

    public ScorerResult score(TraceRequest request) {
        String prompt = PROMPT_TEMPLATE.formatted(
                request.getQuestion(),
                request.getAnswer()
        );

        try {
            String response = chatClient.prompt()
                    .user(prompt)
                    .call()
                    .content();

            return parseResponse(response, "AnswerRelevanceScorer");
        } catch (Exception e) {
            log.error("AnswerRelevanceScorer failed", e);
            return ScorerResult.error("AnswerRelevanceScorer failed: " + e.getMessage());
        }
    }
}
