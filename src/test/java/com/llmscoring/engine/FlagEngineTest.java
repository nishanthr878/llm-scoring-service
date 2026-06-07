package com.llmscoring.engine;

import com.llmscoring.config.ScoringConfig;
import com.llmscoring.enums.ScoringType;
import com.llmscoring.model.ScoringResult;
import com.llmscoring.scorer.Scorer;
import com.llmscoring.scorer.ScorerRegistry;
import com.llmscoring.scorer.ScorerResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FlagEngineTest {

    @Mock
    private ScorerRegistry scorerRegistry;

    @Mock
    private Scorer faithfulnessScorer;

    @Mock
    private Scorer answerRelevanceScorer;

    @Mock
    private Scorer hallucinationScorer;

    private ScoringConfig scoringConfig;
    private FlagEngine flagEngine;

    @BeforeEach
    void setUp() {
        scoringConfig = new ScoringConfig();
        scoringConfig.setThresholds(new ScoringConfig.Thresholds());
        flagEngine = new FlagEngine(scorerRegistry, scoringConfig);

        // Wire mock scorers
        when(faithfulnessScorer.name()).thenReturn("faithfulness");
        when(answerRelevanceScorer.name()).thenReturn("answerRelevance");
        when(hallucinationScorer.name()).thenReturn("hallucination");

        when(scorerRegistry.get(anyList())).thenReturn(List.of(
                faithfulnessScorer,
                answerRelevanceScorer,
                hallucinationScorer
        ));
    }

    private Map<String, Object> buildContext() {
        Map<String, Object> context = new HashMap<>();
        context.put("sessionId", "test-session");
        context.put("modelName", "test-model");
        context.put("promptVersion", "v1.0");
        context.put("latencyMs", 100L);
        context.put("inputTokens", 50);
        context.put("outputTokens", 20);
        context.put("costUsd", 0.001);
        context.put("turnCount", 1);
        return context;
    }

    @Test
    void evaluate_shouldPassWhenAllScorersPass() {
        when(faithfulnessScorer.score(any())).thenReturn(
                new ScorerResult(0.9, "Faithful", true));
        when(answerRelevanceScorer.score(any())).thenReturn(
                new ScorerResult(0.8, "Relevant", true));
        when(hallucinationScorer.score(any())).thenReturn(
                new ScorerResult(0.1, "No hallucination", true));

        ScoringResult result = flagEngine.evaluate(
                buildContext(), ScoringType.SINGLE_TURN);

        assertThat(result.getOverallPassed()).isTrue();
        assertThat(result.getFlagReasons()).isNull();
        assertThat(result.getScores()).containsEntry("faithfulness", 0.9);
        assertThat(result.getScores()).containsEntry("answerRelevance", 0.8);
        assertThat(result.getScores()).containsEntry("hallucination", 0.1);
    }

    @Test
    void evaluate_shouldFailWhenFaithfulnessBelowThreshold() {
        when(faithfulnessScorer.score(any())).thenReturn(
                new ScorerResult(0.3, "Not faithful", true));
        when(answerRelevanceScorer.score(any())).thenReturn(
                new ScorerResult(0.9, "Relevant", true));
        when(hallucinationScorer.score(any())).thenReturn(
                new ScorerResult(0.1, "No hallucination", true));

        ScoringResult result = flagEngine.evaluate(
                buildContext(), ScoringType.SINGLE_TURN);

        assertThat(result.getOverallPassed()).isFalse();
        assertThat(result.getFlagReasons()).contains("faithfulness");
        assertThat(result.getPassed()).containsEntry("faithfulness", false);
        assertThat(result.getPassed()).containsEntry("answerRelevance", true);
    }

    @Test
    void evaluate_shouldFailWhenHallucinationAboveThreshold() {
        when(faithfulnessScorer.score(any())).thenReturn(
                new ScorerResult(0.9, "Faithful", true));
        when(answerRelevanceScorer.score(any())).thenReturn(
                new ScorerResult(0.9, "Relevant", true));
        when(hallucinationScorer.score(any())).thenReturn(
                new ScorerResult(0.8, "High hallucination", true));

        ScoringResult result = flagEngine.evaluate(
                buildContext(), ScoringType.SINGLE_TURN);

        assertThat(result.getOverallPassed()).isFalse();
        assertThat(result.getFlagReasons()).contains("hallucination");
        assertThat(result.getPassed()).containsEntry("hallucination", false);
    }

    @Test
    void evaluate_shouldFlagCostSpike() {
        when(faithfulnessScorer.score(any())).thenReturn(
                new ScorerResult(0.9, "Faithful", true));
        when(answerRelevanceScorer.score(any())).thenReturn(
                new ScorerResult(0.9, "Relevant", true));
        when(hallucinationScorer.score(any())).thenReturn(
                new ScorerResult(0.1, "No hallucination", true));

        Map<String, Object> context = buildContext();
        context.put("costUsd", 0.50); // above 0.10 threshold

        ScoringResult result = flagEngine.evaluate(context, ScoringType.SINGLE_TURN);

        assertThat(result.getOverallPassed()).isFalse();
        assertThat(result.getFlagReasons()).contains("Cost spike");
    }

    @Test
    void evaluate_shouldNotPenalizeBotWhenScorerErrors() {
        when(faithfulnessScorer.score(any())).thenReturn(
                ScorerResult.error("LLM timeout"));
        when(answerRelevanceScorer.score(any())).thenReturn(
                new ScorerResult(0.9, "Relevant", true));
        when(hallucinationScorer.score(any())).thenReturn(
                new ScorerResult(0.1, "No hallucination", true));

        ScoringResult result = flagEngine.evaluate(
                buildContext(), ScoringType.SINGLE_TURN);

        // faithfulness errored — should not count as bot failure
        assertThat(result.getPassed()).containsEntry("faithfulness", null);
        assertThat(result.getPassed()).containsEntry("answerRelevance", true);
        assertThat(result.getPassed()).containsEntry("hallucination", true);
    }

    @Test
    void evaluate_shouldReturnNullOverallPassedWhenAllScorersError() {
        when(faithfulnessScorer.score(any())).thenReturn(
                ScorerResult.error("timeout"));
        when(answerRelevanceScorer.score(any())).thenReturn(
                ScorerResult.error("timeout"));
        when(hallucinationScorer.score(any())).thenReturn(
                ScorerResult.error("timeout"));

        ScoringResult result = flagEngine.evaluate(
                buildContext(), ScoringType.SINGLE_TURN);

        // all scorers errored — inconclusive
        assertThat(result.getOverallPassed()).isNull();
    }

    @Test
    void evaluate_shouldPopulateMetadata() {
        when(faithfulnessScorer.score(any())).thenReturn(
                new ScorerResult(0.9, "Faithful", true));
        when(answerRelevanceScorer.score(any())).thenReturn(
                new ScorerResult(0.9, "Relevant", true));
        when(hallucinationScorer.score(any())).thenReturn(
                new ScorerResult(0.1, "No hallucination", true));

        ScoringResult result = flagEngine.evaluate(
                buildContext(), ScoringType.SINGLE_TURN);

        assertThat(result.getSessionId()).isEqualTo("test-session");
        assertThat(result.getModelName()).isEqualTo("test-model");
        assertThat(result.getType()).isEqualTo(ScoringType.SINGLE_TURN);
        assertThat(result.getLatencyMs()).isEqualTo(100L);
        assertThat(result.getTurnCount()).isEqualTo(1);
    }

    @Test
    void evaluate_shouldPopulateReasoningForAllScorers() {
        when(faithfulnessScorer.score(any())).thenReturn(
                new ScorerResult(0.9, "Very faithful", true));
        when(answerRelevanceScorer.score(any())).thenReturn(
                new ScorerResult(0.8, "Quite relevant", true));
        when(hallucinationScorer.score(any())).thenReturn(
                new ScorerResult(0.0, "No issues found", true));

        ScoringResult result = flagEngine.evaluate(
                buildContext(), ScoringType.SINGLE_TURN);

        assertThat(result.getReasoning())
                .containsEntry("faithfulness", "Very faithful")
                .containsEntry("answerRelevance", "Quite relevant")
                .containsEntry("hallucination", "No issues found");
    }
}
