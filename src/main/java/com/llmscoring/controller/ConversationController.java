package com.llmscoring.controller;

import com.llmscoring.model.Alert;
import com.llmscoring.model.Conversation;
import com.llmscoring.model.ScoringResult;
import com.llmscoring.model.TurnFlag;
import com.llmscoring.repository.AlertRepository;
import com.llmscoring.repository.ScoringResultRepository;
import com.llmscoring.repository.TurnFlagRepository;
import com.llmscoring.service.ConversationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/conversations")
@RequiredArgsConstructor
public class ConversationController {

    private final ConversationService conversationService;
    private final ScoringResultRepository scoringResultRepository;
    private final TurnFlagRepository turnFlagRepository;
    private final AlertRepository alertRepository;

    @GetMapping
    public ResponseEntity<List<Conversation>> getAll(
            @RequestParam(required = false) String filter) {
        return switch (filter != null ? filter : "all") {
            case "failed"  -> ResponseEntity.ok(conversationService.getFailed());
            case "flagged" -> ResponseEntity.ok(conversationService.getFlagged());
            default        -> ResponseEntity.ok(conversationService.getAll());
        };
    }

    @GetMapping("/{id}")
    public ResponseEntity<Conversation> getById(@PathVariable Long id) {
        return ResponseEntity.ok(conversationService.getById(id));
    }

    // Full detail for a conversation — scores + flags + alerts
    @GetMapping("/{id}/detail")
    public ResponseEntity<Map<String, Object>> getDetail(@PathVariable Long id) {
        Conversation conv = conversationService.getById(id);

        ScoringResult scoringResult = conv.getScoringResultId() != null
                ? scoringResultRepository.findById(conv.getScoringResultId()).orElse(null)
                : null;

        List<TurnFlag> flags = turnFlagRepository.findByScoringResultId(conv.getScoringResultId());
        List<Alert> alerts = alertRepository.findBySessionId(conv.getSessionId());

        return ResponseEntity.ok(Map.of(
                "conversation", conv,
                "scoringResult", scoringResult != null ? scoringResult : Map.of(),
                "flags", flags,
                "alerts", alerts
        ));
    }

    @GetMapping("/scenario/{scenarioName}")
    public ResponseEntity<List<Conversation>> getByScenario(
            @PathVariable String scenarioName) {
        return ResponseEntity.ok(conversationService.getByScenario(scenarioName));
    }
}
