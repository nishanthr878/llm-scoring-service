package com.llmscoring.scorer;

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
class HallucinationDetectorTest {

    @Mock
    private ChatClient chatClient;

    @Mock
    private ChatClient.ChatClientRequestSpec requestSpec;

    @Mock
    private ChatClient.CallResponseSpec callSpec;

    private HallucinationDetector detector;

    @BeforeEach
    void setUp() {
        detector = new HallucinationDetector(chatClient);
    }

    private void mockLLMResponse(String response) {
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.user(any(String.class))).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callSpec);
        when(callSpec.content()).thenReturn(response);
    }

    @Test
    void name_shouldReturnHallucination() {
        assertThat(detector.name()).isEqualTo("hallucination");
    }

    @Test
    void score_shouldReturnZeroForNoHallucination() {
        mockLLMResponse("""
                {
                  "score": 0.0,
                  "reasoning": "No fabricated facts detected."
                }
                """);

        Map<String, Object> context = Map.of(
                "question", "What is the capital of France?",
                "answer", "The capital of France is Paris.",
                "retrievedChunks", List.of("Paris is the capital of France.")
        );

        ScorerResult result = detector.score(context);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getScore()).isEqualTo(0.0);
    }

    @Test
    void score_shouldReturnOneForSevereHallucination() {
        mockLLMResponse("""
                {
                  "score": 1.0,
                  "reasoning": "Answer contains completely fabricated facts."
                }
                """);

        Map<String, Object> context = Map.of(
                "question", "What is the capital of France?",
                "answer", "The capital of France is Bengaluru.",
                "retrievedChunks", List.of("Paris is the capital of France.")
        );

        ScorerResult result = detector.score(context);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getScore()).isEqualTo(1.0);
    }

    @Test
    void score_shouldReturnErrorOnLLMFailure() {
        when(chatClient.prompt()).thenThrow(
                new RuntimeException("Rate limit exceeded"));

        Map<String, Object> context = Map.of(
                "question", "test",
                "answer", "test",
                "retrievedChunks", List.of("test context")
        );

        ScorerResult result = detector.score(context);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getReasoning()).contains("failed");
    }
}
