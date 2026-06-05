package com.llmscoring.engine;

import com.llmscoring.config.ScoringConfig;
import com.llmscoring.dto.TraceRequest;
import com.llmscoring.model.ScoringResult;
import com.llmscoring.scorer.AnswerRelevanceScorer;
import com.llmscoring.scorer.FaithfulnessScorer;
import com.llmscoring.scorer.HallucinationDetector;
import com.llmscoring.scorer.ScorerResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@Component
@RequiredArgsConstructor
public class FlagEngine {

    private final FaithfulnessScorer faithfulnessScorer;
    private final AnswerRelevanceScorer answerRelevanceScorer;
    private final HallucinationDetector hallucinationDetector;
    private final ScoringConfig scoringConfig;

    // Virtual thread executor — each task gets its own cheap virtual thread
    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

    public ScoringResult evaluate(TraceRequest request) {
        log.info("Running on virtual thread: {}", Thread.currentThread().isVirtual());
        ScoringResult result = new ScoringResult();

        // Copy trace metadata
        result.setSessionId(request.getSessionId());
        result.setModelName(request.getModelName());
        result.setPromptVersion(request.getPromptVersion());
        result.setQuestion(request.getQuestion());
        result.setAnswer(request.getAnswer());
        result.setLatencyMs(request.getLatencyMs());
        result.setInputTokens(request.getInputTokens());
        result.setOutputTokens(request.getOutputTokens());
        result.setCostUsd(request.getCostUsd());

        // Fire all three scorers concurrently on virtual threads
        long start = System.currentTimeMillis();

        CompletableFuture<ScorerResult> faithfulnessFuture = CompletableFuture
                .supplyAsync(() -> faithfulnessScorer.score(request), executor);

        CompletableFuture<ScorerResult> relevanceFuture = CompletableFuture
                .supplyAsync(() -> answerRelevanceScorer.score(request), executor);

        CompletableFuture<ScorerResult> hallucinationFuture = CompletableFuture
                .supplyAsync(() -> hallucinationDetector.score(request), executor);

        // Wait for all three to complete
        CompletableFuture.allOf(faithfulnessFuture, relevanceFuture, hallucinationFuture).join();

        long scoringLatency = System.currentTimeMillis() - start;
        log.info("All scorers completed in {}ms", scoringLatency);

        ScoringConfig.Thresholds thresholds = scoringConfig.getThresholds();
        List<String> flagReasons = new ArrayList<>();

        // --- Faithfulness ---
        ScorerResult faithfulness = faithfulnessFuture.join();
        result.setFaithfulnessScore(faithfulness.getScore());
        result.setFaithfulnessReasoning(faithfulness.getReasoning());
        boolean faithfulnessPassed = faithfulness.isSuccess() &&
                faithfulness.getScore() >= thresholds.getFaithfulness();
        result.setFaithfulnessPassed(faithfulnessPassed);
        if (!faithfulnessPassed) {
            flagReasons.add(String.format(
                    "Faithfulness too low: %.2f (threshold: %.2f) — %s",
                    faithfulness.getScore(),
                    thresholds.getFaithfulness(),
                    faithfulness.getReasoning()
            ));
        }

        // --- Answer Relevance ---
        ScorerResult relevance = relevanceFuture.join();
        result.setAnswerRelevanceScore(relevance.getScore());
        result.setAnswerRelevanceReasoning(relevance.getReasoning());
        boolean relevancePassed = relevance.isSuccess() &&
                relevance.getScore() >= thresholds.getAnswerRelevance();
        result.setAnswerRelevancePassed(relevancePassed);
        if (!relevancePassed) {
            flagReasons.add(String.format(
                    "Answer relevance too low: %.2f (threshold: %.2f) — %s",
                    relevance.getScore(),
                    thresholds.getAnswerRelevance(),
                    relevance.getReasoning()
            ));
        }

        // --- Hallucination ---
        ScorerResult hallucination = hallucinationFuture.join();
        result.setHallucinationScore(hallucination.getScore());
        result.setHallucinationReasoning(hallucination.getReasoning());
        boolean hallucinationPassed = hallucination.isSuccess() &&
                hallucination.getScore() <= thresholds.getHallucination();
        result.setHallucinationPassed(hallucinationPassed);
        if (!hallucinationPassed) {
            flagReasons.add(String.format(
                    "Hallucination too high: %.2f (threshold: %.2f) — %s",
                    hallucination.getScore(),
                    thresholds.getHallucination(),
                    hallucination.getReasoning()
            ));
        }

        // --- Cost spike check ---
        if (request.getCostUsd() != null && request.getCostUsd() > 0.10) {
            flagReasons.add(String.format(
                    "Cost spike detected: $%.4f exceeds $0.10 threshold",
                    request.getCostUsd()
            ));
        }

        // --- Overall ---
        result.setOverallPassed(faithfulnessPassed && relevancePassed && hallucinationPassed);
        result.setFlagReasons(flagReasons.isEmpty() ? null : String.join(" | ", flagReasons));

        log.info("Evaluation complete — session={} overall={} flags={} scoringLatencyMs={}",
                request.getSessionId(),
                result.getOverallPassed(),
                flagReasons.size(),
                scoringLatency
        );

        return result;
    }
}
