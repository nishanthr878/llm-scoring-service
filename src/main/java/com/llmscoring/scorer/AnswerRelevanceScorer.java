package com.llmscoring.scorer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class AnswerRelevanceScorer extends BaseScorer implements Scorer {

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
            
            Respond ONLY in this exact JSON format, nothing else:
            {
              "score": <float between 0.0 and 1.0>,
              "reasoning": "<one sentence explanation>"
            }
            """;

    @Override
    public String name() {
        return "answerRelevance";
    }

    @Override
    public ScorerResult score(Map<String, Object> context) {
        String question = (String) context.get("question");
        String answer = (String) context.get("answer");

        String prompt = PROMPT_TEMPLATE.formatted(question, answer);

        try {
            String response = callWithRetry(chatClient, prompt, name());
            return parseResponse(response, name());
        } catch (Exception e) {
            log.error("AnswerRelevanceScorer failed", e);
            return ScorerResult.error("AnswerRelevanceScorer failed: " + e.getMessage());
        }
    }
}
