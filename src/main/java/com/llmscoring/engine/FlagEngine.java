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

    // Scorers to run for single turn requests
    private static final List<String> SINGLE_TURN_SCORERS = List.of(
            "faithfulness",
            "answerRelevance",
            "hallucination"
    );

    // Scorers to run for conversation requests (we'll add these next)
    private static final List<String> CONVERSATION_SCORERS = List.of(
            "consistency",
            "contextRetention",
            "conversationFaithfulness"
    );

    private static final List<String> SCENARIO_SCORERS = List.of(
            "policyCompliance",
            "expectedBehavior"
    );

    public ScoringResult evaluate(Map<String, Object> context, ScoringType type) {
        ScoringResult result = new ScoringResult();

        // Copy metadata from context
        result.setSessionId((String) context.get("sessionId"));
        result.setModelName((String) context.get("modelName"));
        result.setPromptVersion((String) context.get("promptVersion"));
        result.setType(type);
        result.setLatencyMs((Long) context.get("latencyMs"));
        result.setInputTokens((Integer) context.get("inputTokens"));
        result.setOutputTokens((Integer) context.get("outputTokens"));
        result.setCostUsd((Double) context.get("costUsd"));
        result.setTurnCount((Integer) context.getOrDefault("turnCount", 1));

        // Pick scorers based on type
        List<String> scorerNames = switch (type) {
            case SINGLE_TURN -> SINGLE_TURN_SCORERS;
            case CONVERSATION -> CONVERSATION_SCORERS;
            case SCENARIO -> SCENARIO_SCORERS;
        };

        List<Scorer> scorers = scorerRegistry.get(scorerNames);

        // Run all scorers concurrently on virtual threads
        long start = System.currentTimeMillis();

        Map<String, CompletableFuture<ScorerResult>> futures = new HashMap<>();
        for (Scorer scorer : scorers) {
            futures.put(scorer.name(), CompletableFuture
                    .supplyAsync(() -> scorer.score(context), executor));
        }

        CompletableFuture.allOf(futures.values().toArray(new CompletableFuture[0])).join();

        log.info("All scorers completed in {}ms", System.currentTimeMillis() - start);

        // Collect results
        Map<String, Double> scores = new HashMap<>();
        Map<String, String> reasoning = new HashMap<>();
        Map<String, Boolean> passed = new HashMap<>();
        List<String> flagReasons = new ArrayList<>();

        ScoringConfig.Thresholds thresholds = scoringConfig.getThresholds();

        for (Map.Entry<String, CompletableFuture<ScorerResult>> entry : futures.entrySet()) {
            String scorerName = entry.getKey();
            ScorerResult scorerResult = entry.getValue().join();

            scores.put(scorerName, scorerResult.getScore());
            reasoning.put(scorerName, scorerResult.getReasoning());

            // Hallucination is inverted — high score is bad
            boolean scorerPassed = scorerResult.isSuccess() && (
                    scorerName.equals("hallucination")
                            ? scorerResult.getScore() <= thresholds.getHallucination()
                            : scorerResult.getScore() >= thresholds.getFaithfulness()
            );

            passed.put(scorerName, scorerPassed);

            if (!scorerPassed) {
                flagReasons.add(String.format(
                        "%s failed: %.2f — %s",
                        scorerName,
                        scorerResult.getScore(),
                        scorerResult.getReasoning()
                ));
            }
        }

        // Cost spike check
        Double costUsd = (Double) context.get("costUsd");
        if (costUsd != null && costUsd > 0.10) {
            flagReasons.add(String.format("Cost spike: $%.4f exceeds $0.10", costUsd));
        }

        result.setScores(scores);
        result.setReasoning(reasoning);
        result.setPassed(passed);
        result.setOverallPassed(flagReasons.isEmpty());
        result.setFlagReasons(flagReasons.isEmpty() ? null : String.join(" | ", flagReasons));

        log.info("Evaluation complete — session={} type={} overall={} flags={}",
                result.getSessionId(), type, result.getOverallPassed(), flagReasons.size());

        return result;
    }
}
