package com.llmscoring.controller;

import com.llmscoring.dto.ConversationRequest;
import com.llmscoring.dto.RawConversationRequest;
import com.llmscoring.dto.ScenarioRequest;
import com.llmscoring.dto.TraceRequest;
import com.llmscoring.model.ScoringResult;
import com.llmscoring.service.ScoringService;
import com.llmscoring.util.ChatLogParser;
import com.llmscoring.dto.ScenarioEvaluationRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/scoring")
@RequiredArgsConstructor
public class ScoringController {

    private final ScoringService scoringService;

    @PostMapping("/evaluate")
    public ResponseEntity<ScoringResult> evaluate(@Valid @RequestBody TraceRequest request) {
        return ResponseEntity.ok(scoringService.evaluate(request));
    }

    @PostMapping("/evaluate/conversation")
    public ResponseEntity<ScoringResult> evaluateConversation(
            @Valid @RequestBody ConversationRequest request) {
        return ResponseEntity.ok(scoringService.evaluateConversation(request));
    }

    @GetMapping("/results")
    public ResponseEntity<List<ScoringResult>> getAll() {
        return ResponseEntity.ok(scoringService.getAll());
    }

    @GetMapping("/results/failed")
    public ResponseEntity<List<ScoringResult>> getFailed() {
        return ResponseEntity.ok(scoringService.getFailed());
    }

    @GetMapping("/results/session/{sessionId}")
    public ResponseEntity<List<ScoringResult>> getBySession(@PathVariable String sessionId) {
        return ResponseEntity.ok(scoringService.getBySession(sessionId));
    }

    @GetMapping("/results/{id}")
    public ResponseEntity<ScoringResult> getById(@PathVariable Long id) {
        return ResponseEntity.ok(scoringService.getById(id));
    }

    @PostMapping("/evaluate/conversation/raw")
    public ResponseEntity<ScoringResult> evaluateRaw(
            @RequestBody RawConversationRequest request
    ) {
        ConversationRequest parsed = ChatLogParser.parse(
                request.getRawLog(),
                request.getRetrievedChunks(),
                request.getSessionId(),
                request.getModelName(),
                request.getPromptVersion()
        );
        return ResponseEntity.ok(scoringService.evaluateConversation(parsed));
    }


    @PostMapping("/evaluate/scenario")
    public ResponseEntity<ScoringResult> evaluateScenario(
            @Valid @RequestBody ScenarioRequest request) {
        return ResponseEntity.ok(scoringService.evaluateScenario(request));
    }

    @PostMapping("/evaluate/scenario/{scenarioName}")
    public ResponseEntity<ScoringResult> evaluateWithScenario(
            @PathVariable String scenarioName,
            @RequestBody ScenarioEvaluationRequest request) {
        return ResponseEntity.ok(
                scoringService.evaluateWithScenario(
                        scenarioName, request
                ));
    }
}
