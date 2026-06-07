package com.llmscoring.scorer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BaseScorerTest {

    // Concrete subclass for testing abstract BaseScorer
    private static class TestScorer extends BaseScorer {
        public ScorerResult testParse(String response) {
            return parseResponse(response, "test");
        }
    }

    private TestScorer scorer;

    @BeforeEach
    void setUp() {
        scorer = new TestScorer();
    }

    @Test
    void parseResponse_shouldParseValidJson() {
        String response = """
                {
                  "score": 0.85,
                  "reasoning": "Good answer."
                }
                """;

        ScorerResult result = scorer.testParse(response);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getScore()).isEqualTo(0.85);
        assertThat(result.getReasoning()).isEqualTo("Good answer.");
    }

    @Test
    void parseResponse_shouldStripMarkdownCodeFences() {
        String response = """
```json
                {
                  "score": 0.9,
                  "reasoning": "Excellent."
                }
```
                """;

        ScorerResult result = scorer.testParse(response);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getScore()).isEqualTo(0.9);
    }

    @Test
    void parseResponse_shouldHandleScoreOfZero() {
        String response = """
                {
                  "score": 0.0,
                  "reasoning": "Completely wrong."
                }
                """;

        ScorerResult result = scorer.testParse(response);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getScore()).isEqualTo(0.0);
    }

    @Test
    void parseResponse_shouldHandleScoreOfOne() {
        String response = """
                {
                  "score": 1.0,
                  "reasoning": "Perfect."
                }
                """;

        ScorerResult result = scorer.testParse(response);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getScore()).isEqualTo(1.0);
    }

    @Test
    void parseResponse_shouldReturnErrorForGarbage() {
        String response = "this is not json at all";

        ScorerResult result = scorer.testParse(response);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getScore()).isEqualTo(0.0);
    }

    @Test
    void parseResponse_shouldReturnErrorForEmptyString() {
        ScorerResult result = scorer.testParse("");

        assertThat(result.isSuccess()).isFalse();
    }

    @Test
    void error_shouldReturnFailedResult() {
        ScorerResult result = ScorerResult.error("Something went wrong");

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getScore()).isEqualTo(0.0);
        assertThat(result.getReasoning()).isEqualTo("Something went wrong");
    }
}
