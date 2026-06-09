package com.llmscoring.service;

import com.llmscoring.enums.FlagType;
import com.llmscoring.enums.Severity;
import com.llmscoring.model.Alert;
import com.llmscoring.model.ScoringResult;
import com.llmscoring.model.TurnFlag;
import com.llmscoring.repository.AlertRepository;
import com.llmscoring.repository.TurnFlagRepository;
import com.llmscoring.scorer.ScorerResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class TurnFlagService {

    private final TurnFlagRepository turnFlagRepository;
    private final AlertRepository alertRepository;

    public List<TurnFlag> processFlags(
            ScoringResult scoringResult,
            Map<String, ScorerResult> scorerResults,
            Map<String, Object> context) {

        List<TurnFlag> flags = new ArrayList<>();
        String sessionId = scoringResult.getSessionId();
        String scenarioName = (String) context.getOrDefault("scenarioName", "unknown");

        // Process turn-level hallucination flags
        ScorerResult turnHallucination = scorerResults.get("turnHallucination");
        if (turnHallucination != null && turnHallucination.getDetails() != null) {
            for (Map<String, Object> turn : turnHallucination.getDetails()) {
                double score = ((Number) turn.get("score")).doubleValue();
                if (score >= 0.7) {
                    TurnFlag flag = buildFlag(
                            scoringResult, sessionId, scenarioName,
                            ((Number) turn.get("turnIndex")).intValue(),
                            (String) turn.getOrDefault("content", ""),
                            "assistant",
                            FlagType.HALLUCINATION,
                            score >= 0.9 ? Severity.CRITICAL : Severity.HIGH,
                            "turnHallucination",
                            score,
                            (String) turn.get("reasoning"),
                            null, null
                    );
                    flags.add(turnFlagRepository.save(flag));
                    createAlert(flag, scoringResult, 0.7);
                }
            }
        }

        // Process PII detection flags
        ScorerResult piiResult = scorerResults.get("piiDetection");
        if (piiResult != null && piiResult.getDetails() != null) {
            for (Map<String, Object> detection : piiResult.getDetails()) {
                String severityStr = (String) detection.getOrDefault("severity", "MEDIUM");
                TurnFlag flag = buildFlag(
                        scoringResult, sessionId, scenarioName,
                        ((Number) detection.get("turnIndex")).intValue(),
                        "",
                        (String) detection.getOrDefault("role", "unknown"),
                        FlagType.PII_DETECTED,
                        Severity.valueOf(severityStr),
                        "piiDetection",
                        0.0,
                        String.format("%s detected: %s",
                                detection.get("piiType"),
                                detection.get("maskedValue")),
                        (String) detection.get("maskedValue"),
                        null
                );
                flags.add(turnFlagRepository.save(flag));
                createAlert(flag, scoringResult, 1.0);
            }
        }

        // Process custom pattern flags
        ScorerResult patternResult = scorerResults.get("customPattern");
        if (patternResult != null && patternResult.getDetails() != null) {
            for (Map<String, Object> match : patternResult.getDetails()) {
                String severityStr = (String) match.getOrDefault("severity", "MEDIUM");
                TurnFlag flag = buildFlag(
                        scoringResult, sessionId, scenarioName,
                        ((Number) match.get("turnIndex")).intValue(),
                        (String) match.getOrDefault("matchedText", ""),
                        (String) match.getOrDefault("role", "unknown"),
                        FlagType.CUSTOM_PATTERN,
                        Severity.valueOf(severityStr),
                        "customPattern",
                        0.0,
                        String.format("Pattern '%s' matched", match.get("patternName")),
                        null,
                        (String) match.get("patternName")
                );
                flags.add(turnFlagRepository.save(flag));

                if (Severity.valueOf(severityStr).ordinal() >= Severity.HIGH.ordinal()) {
                    createAlert(flag, scoringResult, 1.0);
                }
            }
        }

        if (!flags.isEmpty()) {
            log.info("Created {} turn flags for session={}", flags.size(), sessionId);
        }

        return flags;
    }

    private TurnFlag buildFlag(
            ScoringResult result, String sessionId, String scenarioName,
            int turnIndex, String content, String role,
            FlagType flagType, Severity severity, String scorerName,
            double score, String description, String maskedValue, String patternName) {

        TurnFlag flag = new TurnFlag();
        flag.setScoringResultId(result.getId());
        flag.setSessionId(sessionId);
        flag.setScenarioName(scenarioName);
        flag.setTurnIndex(turnIndex);
        flag.setTurnContent(content);
        flag.setRole(role);
        flag.setFlagType(flagType);
        flag.setSeverity(severity);
        flag.setScorerName(scorerName);
        flag.setScore(score);
        flag.setDescription(description);
        flag.setMaskedValue(maskedValue);
        flag.setPatternName(patternName);
        return flag;
    }

    private void createAlert(TurnFlag flag, ScoringResult result, double threshold) {
        Alert alert = new Alert();
        alert.setScoringResultId(result.getId());
        alert.setTurnFlagId(flag.getId());
        alert.setSessionId(flag.getSessionId());
        alert.setScenarioName(flag.getScenarioName());
        alert.setTurnIndex(flag.getTurnIndex());
        alert.setFlagType(flag.getFlagType());
        alert.setSeverity(flag.getSeverity());
        alert.setScorerName(flag.getScorerName());
        alert.setScore(flag.getScore());
        alert.setThreshold(threshold);
        alert.setReason(flag.getDescription());
        alertRepository.save(alert);

        log.info("Alert created — session={} type={} severity={} turn={}",
                flag.getSessionId(), flag.getFlagType(),
                flag.getSeverity(), flag.getTurnIndex());
    }
}
