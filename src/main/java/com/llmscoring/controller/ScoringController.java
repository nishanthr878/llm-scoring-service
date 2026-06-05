package com.llmscoring.controller;

import com.llmscoring.dto.TraceRequest;
import com.llmscoring.model.ScoringResult;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.llmscoring.service.ScoringService;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/scoring")
@RequiredArgsConstructor
public class ScoringController {

    private final ScoringService scoringService;

    // Submit a trace for scoring
    @PostMapping("/evaluate")
    public ResponseEntity<ScoringResult> evaluate(@Valid @RequestBody TraceRequest request) {
        ScoringResult result = scoringService.evaluate(request);
        return ResponseEntity.ok(result);
    }

    // Get all scoring results
    @GetMapping("/results")
    public ResponseEntity<List<ScoringResult>> getAll() {
        return ResponseEntity.ok(scoringService.getAll());
    }

    // Get only failed results — powers the bad answers dashboard
    @GetMapping("/results/failed")
    public ResponseEntity<List<ScoringResult>> getFailed() {
        return ResponseEntity.ok(scoringService.getFailed());
    }

    // Get results by session
    @GetMapping("/results/session/{sessionId}")
    public ResponseEntity<List<ScoringResult>> getBySession(@PathVariable String sessionId) {
        return ResponseEntity.ok(scoringService.getBySession(sessionId));
    }

    // Get single result by id
    @GetMapping("/results/{id}")
    public ResponseEntity<ScoringResult> getById(@PathVariable Long id) {
        return ResponseEntity.ok(scoringService.getById(id));
    }
}
