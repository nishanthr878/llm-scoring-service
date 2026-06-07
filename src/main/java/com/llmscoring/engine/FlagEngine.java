package com.llmscoring.engine;

import com.llmscoring.config.ScoringConfig;
import com.llmscoring.enums.ScoringType;
import com.llmscoring.model.ScoringResult;
import com.llmscoring.scorer.Scorer;
import com.llmscoring.scorer.ScorerRegistry;
import com.llmscoring.scorer.ScorerResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@Component
@RequiredArgsConstructor
public class FlagEngine {

    private final ScorerRegistry scorerRegistry;
    private final ScoringConfig scoringConfig;

    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

    private static final List<String> SINGLE_TURN_SCORERS = List.of(
            "faithfulness", "answerRelevance", "hallucination");

    private static final List<String> CONVERSATION_SCORERS = List.of(
            "consistency", "contextRetention", "conversationFaithfulness");

    // --- Public API ---

    public ScoringResult evaluate(Map<String, Object> context, ScoringType type) {
        List<String> scorerNames = switch (type) {
            case SINGLE_TURN -> SINGLE_TURN_SCORERS;
            case CONVERSATION -> CONVERSATION_SCORERS;
            case SCENARIO -> throw new IllegalArgumentException(
                    "Use evaluateWithScorers() for SCENARIO type");
        };
        return evaluateWithScorers(context, type, scorerNames);
    }

    public ScoringResult evaluateWithScorers(
            Map<String, Object> context,
            ScoringType type,
            List<String> scorerNames) {

        ScoringResult result = buildResult(context, type);
        EvaluationOutput output = runScorers(scorerNames, context);

        result.setScores(output.scores);
        result.setReasoning(output.reasoning);
        result.setPassed(output.passed);
        result.setDetails(output.details.isEmpty() ? null : output.details);
        result.setOverallPassed(output.flagReasons.isEmpty());
        result.setFlagReasons(output.flagReasons.isEmpty()
                ? null : String.join(" | ", output.flagReasons));

        log.info("Evaluation complete — session={} type={} overall={} flags={}",
                result.getSessionId(), type,
                result.getOverallPassed(), output.flagReasons.size());

        return result;
    }

    // --- Private methods ---

    private ScoringResult buildResult(Map<String, Object> context, ScoringType type) {
        ScoringResult result = new ScoringResult();
        result.setSessionId((String) context.get("sessionId"));
        result.setModelName((String) context.get("modelName"));
        result.setPromptVersion((String) context.get("promptVersion"));
        result.setType(type);
        result.setLatencyMs((Long) context.get("latencyMs"));
        result.setInputTokens((Integer) context.get("inputTokens"));
        result.setOutputTokens((Integer) context.get("outputTokens"));
        result.setCostUsd((Double) context.get("costUsd"));
        result.setTurnCount((Integer) context.getOrDefault("turnCount", 1));
        return result;
    }

    private EvaluationOutput runScorers(
            List<String> scorerNames,
            Map<String, Object> context) {

        List<Scorer> scorers = scorerRegistry.get(scorerNames);

        // Fire all scorers concurrently
        long start = System.currentTimeMillis();
        Map<String, CompletableFuture<ScorerResult>> futures = new HashMap<>();
        for (Scorer scorer : scorers) {
            futures.put(scorer.name(), CompletableFuture
                    .supplyAsync(() -> scorer.score(context), executor));
        }
        CompletableFuture.allOf(futures.values().toArray(new CompletableFuture[0])).join();
        log.info("All scorers completed in {}ms", System.currentTimeMillis() - start);

        // Collect results
        EvaluationOutput output = new EvaluationOutput();
        ScoringConfig.Thresholds thresholds = scoringConfig.getThresholds();

        for (Map.Entry<String, CompletableFuture<ScorerResult>> entry : futures.entrySet()) {
            String scorerName = entry.getKey();
            ScorerResult scorerResult = entry.getValue().join();

            output.scores.put(scorerName, scorerResult.getScore());
            output.reasoning.put(scorerName, scorerResult.getReasoning());

            boolean scorerPassed = scorerResult.isSuccess() && (
                    scorerName.equals("hallucination")
                            ? scorerResult.getScore() <= thresholds.getHallucination()
                            : scorerResult.getScore() >= thresholds.getFaithfulness()
            );

            output.passed.put(scorerName, scorerPassed);

            if (scorerResult.getDetails() != null) {
                output.details.put(scorerName, scorerResult.getDetails());
            }

            if (!scorerPassed) {
                output.flagReasons.add(String.format("%s failed: %.2f — %s",
                        scorerName, scorerResult.getScore(),
                        scorerResult.getReasoning()));
            }
        }

        // Cost spike check
        Double costUsd = (Double) context.get("costUsd");
        if (costUsd != null && costUsd > 0.10) {
            output.flagReasons.add(String.format(
                    "Cost spike: $%.4f exceeds $0.10", costUsd));
        }

        return output;
    }

    // Simple data carrier for runScorers output
    private static class EvaluationOutput {
        Map<String, Double> scores = new HashMap<>();
        Map<String, String> reasoning = new HashMap<>();
        Map<String, Boolean> passed = new HashMap<>();
        Map<String, Object> details = new HashMap<>();
        List<String> flagReasons = new ArrayList<>();
    }
}