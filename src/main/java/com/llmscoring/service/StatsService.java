package com.llmscoring.service;

import com.llmscoring.dto.stats.*;
import com.llmscoring.enums.ScoringType;
import com.llmscoring.model.ScoringResult;
import com.llmscoring.repository.ScoringResultRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class StatsService {

    private final ScoringResultRepository repository;

    public OverviewStats getOverview() {
        long total = repository.count();
        long passed = repository.countByOverallPassedTrue();
        long failed = repository.countByOverallPassedFalse();
        long inconclusive = repository.countByOverallPassedIsNull();
        double passRate = total == 0 ? 0.0 : (double) passed / total * 100;

        return OverviewStats.builder()
                .totalEvaluations(total)
                .totalPassed(passed)
                .totalFailed(failed)
                .totalInconclusive(inconclusive)
                .passRate(Math.round(passRate * 100.0) / 100.0)
                .singleTurnCount(repository.countByType(ScoringType.SINGLE_TURN))
                .conversationCount(repository.countByType(ScoringType.CONVERSATION))
                .scenarioCount(repository.countByType(ScoringType.SCENARIO))
                .build();
    }

    public List<ModelStats> getByModel() {
        List<String> models = repository.findDistinctModelNames();

        return models.stream()
                .filter(Objects::nonNull)
                .map(model -> {
                    List<ScoringResult> results = repository.findByModelName(model);
                    return buildModelStats(model, results);
                })
                .collect(Collectors.toList());
    }

    public List<TrendPoint> getTrends(int days) {
        Instant from = Instant.now().minus(days, ChronoUnit.DAYS);
        List<Object[]> raw = repository.findDailyTrend(from);

        return raw.stream().map(row -> {
            String date = row[0].toString();
            long total = ((Number) row[1]).longValue();
            long passed = ((Number) row[2]).longValue();
            long failed = ((Number) row[3]).longValue();
            double passRate = total == 0 ? 0.0
                    : Math.round((double) passed / total * 10000.0) / 100.0;

            return TrendPoint.builder()
                    .date(date)
                    .total(total)
                    .passed(passed)
                    .failed(failed)
                    .passRate(passRate)
                    .build();
        }).collect(Collectors.toList());
    }

    public List<FailureStats> getFailureStats() {
        List<ScoringResult> failed = repository.findByOverallPassedFalse();

        // Count failures per scorer
        Map<String, List<Double>> scorerFailures = new HashMap<>();

        for (ScoringResult result : failed) {
            if (result.getPassed() == null) continue;

            result.getPassed().forEach((scorerName, passed) -> {
                if (Boolean.FALSE.equals(passed)) {
                    scorerFailures.computeIfAbsent(scorerName, k -> new ArrayList<>())
                            .add(result.getScores() != null
                                    ? result.getScores().getOrDefault(scorerName, 0.0)
                                    : 0.0);
                }
            });
        }

        return scorerFailures.entrySet().stream()
                .map(entry -> {
                    List<Double> scores = entry.getValue();
                    double avgScore = scores.stream()
                            .mapToDouble(Double::doubleValue)
                            .average()
                            .orElse(0.0);

                    return FailureStats.builder()
                            .scorerName(entry.getKey())
                            .failureCount(scores.size())
                            .avgScore(Math.round(avgScore * 100.0) / 100.0)
                            .build();
                })
                .sorted(Comparator.comparingLong(FailureStats::getFailureCount).reversed())
                .collect(Collectors.toList());
    }

    private ModelStats buildModelStats(String model, List<ScoringResult> results) {
        long total = results.size();
        long passed = results.stream()
                .filter(r -> Boolean.TRUE.equals(r.getOverallPassed()))
                .count();
        long failed = results.stream()
                .filter(r -> Boolean.FALSE.equals(r.getOverallPassed()))
                .count();
        double passRate = total == 0 ? 0.0
                : Math.round((double) passed / total * 10000.0) / 100.0;

        // Avg score per scorer across all results for this model
        Map<String, List<Double>> scorerScores = new HashMap<>();
        for (ScoringResult result : results) {
            if (result.getScores() == null) continue;
            result.getScores().forEach((scorer, score) ->
                    scorerScores.computeIfAbsent(scorer, k -> new ArrayList<>())
                            .add(score));
        }

        Map<String, Double> avgScores = new HashMap<>();
        scorerScores.forEach((scorer, scores) -> {
            double avg = scores.stream()
                    .mapToDouble(Double::doubleValue)
                    .average()
                    .orElse(0.0);
            avgScores.put(scorer, Math.round(avg * 100.0) / 100.0);
        });

        return ModelStats.builder()
                .modelName(model)
                .totalEvaluations(total)
                .passed(passed)
                .failed(failed)
                .passRate(passRate)
                .avgScores(avgScores)
                .build();
    }
}
