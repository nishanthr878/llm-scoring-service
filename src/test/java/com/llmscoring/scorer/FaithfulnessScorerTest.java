package com.llmscoring.scorer;

import com.llmscoring.dto.TraceRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FaithfulnessScorerTest {

    @Mock
    private ChatClient chatClient;

    @Mock
    private ChatClient.ChatClientRequestSpec requestSpec;

    @Mock
    private ChatClient.CallResponseSpec callSpec;

    private FaithfulnessScorer scorer;

    @BeforeEach
    void setUp() {
        scorer = new FaithfulnessScorer(chatClient);
    }

    private void mockLLMResponse(String response) {
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.user(any(String.class))).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callSpec);
        when(callSpec.content()).thenReturn(response);
    }

    private Map<String, Object> buildContext(String question,
                                              String answer,
                                              List<String> chunks) {
        return Map.of(
                "question", question,
                "answer", answer,
                "retrievedChunks", chunks
        );
    }

    @Test
    void name_shouldReturnFaithfulness() {
        assertThat(scorer.name()).isEqualTo("faithfulness");
    }

    @Test
    void score_shouldReturnHighScoreForFaithfulAnswer() {
        mockLLMResponse("""
                {
                  "score": 0.95,
                  "reasoning": "The answer is fully supported by the context."
                }
                """);

        Map<String, Object> context = buildContext(
                "What is the capital of France?",
                "The capital of France is Paris.",
                List.of("Paris is the capital of France.")
        );

        ScorerResult result = scorer.score(context);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getScore()).isEqualTo(0.95);
        assertThat(result.getReasoning()).contains("supported");
    }

    @Test
    void score_shouldReturnLowScoreForUnfaithfulAnswer() {
        mockLLMResponse("""
                {
                  "score": 0.0,
                  "reasoning": "The answer contradicts the context."
                }
                """);

        Map<String, Object> context = buildContext(
                "What is the capital of France?",
                "The capital of France is Berlin.",
                List.of("Paris is the capital of France.")
        );

        ScorerResult result = scorer.score(context);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getScore()).isEqualTo(0.0);
    }

    @Test
    void score_shouldHandleLLMFailureGracefully() {
        when(chatClient.prompt()).thenThrow(
                new RuntimeException("Connection timeout"));

        Map<String, Object> context = buildContext(
                "What is the capital?",
                "Paris",
                List.of("Paris is the capital.")
        );

        ScorerResult result = scorer.score(context);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getScore()).isEqualTo(0.0);
        assertThat(result.getReasoning()).contains("failed");
    }

    @Test
    void score_shouldHandleMarkdownWrappedJsonResponse() {
        mockLLMResponse("""
```json
                {
                  "score": 0.8,
                  "reasoning": "Mostly faithful."
                }
```
                """);

        Map<String, Object> context = buildContext(
                "What is the capital?",
                "Paris",
                List.of("Paris is the capital.")
        );

        ScorerResult result = scorer.score(context);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getScore()).isEqualTo(0.8);
    }

    @Test
    void score_shouldHandleMultipleChunks() {
        mockLLMResponse("""
                {
                  "score": 1.0,
                  "reasoning": "Fully supported across all chunks."
                }
                """);

        Map<String, Object> context = buildContext(
                "What is the capital?",
                "Paris",
                List.of(
                        "France is in Western Europe.",
                        "Paris is the capital of France.",
                        "Paris has 2.1 million residents."
                )
        );

        ScorerResult result = scorer.score(context);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getScore()).isEqualTo(1.0);
    }
}
