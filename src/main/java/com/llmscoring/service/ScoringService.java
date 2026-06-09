package com.llmscoring.service;

import com.llmscoring.dto.*;
import com.llmscoring.engine.FlagEngine;
import com.llmscoring.enums.ScoringType;
import com.llmscoring.enums.Severity;
import com.llmscoring.model.ScoringResult;
import com.llmscoring.model.TurnFlag;
import com.llmscoring.repository.ScoringResultRepository;
import com.llmscoring.scorer.ScorerResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import com.llmscoring.model.Scenario;
import com.llmscoring.service.ScenarioService;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ScoringService {

    private final FlagEngine flagEngine;
    private final ScoringResultRepository repository;
    private final ScenarioService scenarioService;
    private final TurnFlagService turnFlagService;
    private final SseEmitterService sseEmitterService;
    private final ConversationService conversationService;

    public ScoringResult evaluate(TraceRequest request) {
        log.info("Evaluating single turn — session={} model={}",
                request.getSessionId(), request.getModelName());


        Map<String, Object> context = request.toContext();
        ScoringResult result = flagEngine.evaluate(context, ScoringType.SINGLE_TURN);
        ScoringResult saved = repository.save(result);
        processFlags(saved, flagEngine.getLastScorerResults(), context);

        return saved;
    }

    public ScoringResult evaluateConversation(ConversationRequest request) {
        log.info("Evaluating conversation — session={} turns={}",
                request.getSessionId(), request.getMessages().size());
        Map<String, Object> context = request.toContext();
        ScoringResult result = flagEngine.evaluate(context, ScoringType.CONVERSATION);
        ScoringResult saved = repository.save(result);
        processFlags(saved, flagEngine.getLastScorerResults(), context);
        return saved;
    }

    public List<ScoringResult> getAll() {
        return repository.findAll();
    }

    public List<ScoringResult> getFailed() {
        return repository.findByOverallPassedFalse();
    }

    public List<ScoringResult> getBySession(String sessionId) {
        return repository.findBySessionId(sessionId);
    }

    public ScoringResult getById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RuntimeException("ScoringResult not found: " + id));
    }

    public ScoringResult evaluateScenario(ScenarioRequest request) {
        log.info("Evaluating scenario — name={} session={}",
                request.getScenarioName(), request.getSessionId());
        ScoringResult result = flagEngine.evaluate(
                request.toContext(),
                ScoringType.SCENARIO
        );
        return repository.save(result);
    }


    public ScoringResult evaluateWithScenario(String scenarioName,
                                              ScenarioEvaluationRequest request) {
        Scenario scenario = scenarioService.getByName(scenarioName);

        String formattedConversation = request.getMessages().stream()
                .map(m -> m.getRole().toUpperCase() + ": " + m.getContent())
                .collect(Collectors.joining("\n\n"));

        Map<String, Object> context = new HashMap<>();
        context.put("scenarioName", scenario.getName());
        context.put("policy", scenario.getPolicy());
        context.put("expectedBehaviors", scenario.getExpectedBehaviors());
        context.put("formattedConversation", formattedConversation);
        context.put("messages", request.getMessages());
        context.put("sessionId", request.getSessionId());
        context.put("modelName", request.getModelName() != null
                ? request.getModelName() : "unknown");
        context.put("promptVersion", "v1.0");
        context.put("turnCount", request.getMessages().size());
        context.put("latencyMs", null);
        context.put("inputTokens", null);
        context.put("outputTokens", null);
        context.put("costUsd", null);
        context.put("piiTypesToDetect", scenario.getPiiTypesToDetect() != null
                ? scenario.getPiiTypesToDetect()
                : List.of("EMAIL", "PHONE", "CREDIT_CARD", "SSN", "API_KEY"));
        context.put("customPatterns", scenario.getCustomPatterns());
        context.put("retrievedChunks", List.of()); // empty if not provided

        ScoringResult result = flagEngine.evaluateWithScorers(
                context,
                ScoringType.SCENARIO,
                scenario.getScorerNames()
        );

        ScoringResult saved = repository.save(result);
        processFlags(saved, flagEngine.getLastScorerResults(), context);
        return saved;
    }



    private void processFlags(ScoringResult result,
                              Map<String, ScorerResult> scorerResults,
                              Map<String, Object> context) {
        try {
            List<TurnFlag> flags = turnFlagService.processFlags(result, scorerResults, context);

            // Handle both ChatMessage objects and raw Maps
            List<Map<String, Object>> messageMaps = null;
            Object rawMessages = context.get("messages");

            if (rawMessages instanceof List<?> list && !list.isEmpty()) {
                messageMaps = list.stream()
                        .map(m -> {
                            if (m instanceof com.llmscoring.dto.ChatMessage cm) {
                                return Map.<String, Object>of(
                                        "role", cm.getRole(),
                                        "content", cm.getContent()
                                );
                            } else if (m instanceof Map<?,?> map) {
                                return Map.<String, Object>of(
                                        "role", String.valueOf(map.get("role")),
                                        "content", String.valueOf(map.get("content"))
                                );
                            }
                            return Map.<String, Object>of("role", "unknown", "content", "");
                        })
                        .collect(java.util.stream.Collectors.toList());
            }

            String scenarioName = (String) context.getOrDefault("scenarioName", "unknown");
            conversationService.store(result, messageMaps, scenarioName, flags);

            sseEmitterService.pushScoringResult(result, flags);
            flags.stream()
                    .filter(f -> f.getSeverity().ordinal() >= Severity.HIGH.ordinal())
                    .forEach(sseEmitterService::pushTurnFlag);
        } catch (Exception e) {
            log.error("Flag processing failed", e);
        }
    }



}
