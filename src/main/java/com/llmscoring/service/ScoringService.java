package com.llmscoring.service;

import com.llmscoring.dto.*;
import com.llmscoring.engine.FlagEngine;
import com.llmscoring.enums.ScoringType;
import com.llmscoring.model.ScoringResult;
import com.llmscoring.repository.ScoringResultRepository;
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

    public ScoringResult evaluate(TraceRequest request) {
        log.info("Evaluating single turn — session={} model={}",
                request.getSessionId(), request.getModelName());
        ScoringResult result = flagEngine.evaluate(
                request.toContext(),
                ScoringType.SINGLE_TURN
        );
        return repository.save(result);
    }

    public ScoringResult evaluateConversation(ConversationRequest request) {
        log.info("Evaluating conversation — session={} turns={}",
                request.getSessionId(), request.getMessages().size());
        ScoringResult result = flagEngine.evaluate(
                request.toContext(),
                ScoringType.CONVERSATION
        );
        return repository.save(result);
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

        ScoringResult result = flagEngine.evaluateWithScorers(
                context,
                ScoringType.SCENARIO,
                scenario.getScorerNames()
        );

        return repository.save(result);
    }
}
