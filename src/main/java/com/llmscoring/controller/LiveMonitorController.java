package com.llmscoring.controller;

import com.llmscoring.model.Alert;
import com.llmscoring.model.TurnFlag;
import com.llmscoring.repository.AlertRepository;
import com.llmscoring.repository.TurnFlagRepository;
import com.llmscoring.service.SseEmitterService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

@RestController
@RequestMapping("/api/v1/monitor")
@RequiredArgsConstructor
public class LiveMonitorController {

    private final SseEmitterService sseEmitterService;
    private final AlertRepository alertRepository;
    private final TurnFlagRepository turnFlagRepository;

    // SSE endpoint — UI connects here
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream() {
        return sseEmitterService.createEmitter();
    }

    // Active alerts
    @GetMapping("/alerts")
    public ResponseEntity<List<Alert>> getAlerts() {
        return ResponseEntity.ok(alertRepository.findByDismissedFalseOrderByFiredAtDesc());
    }

    // Dismiss alert
    @PostMapping("/alerts/{id}/dismiss")
    public ResponseEntity<Void> dismissAlert(@PathVariable Long id) {
        alertRepository.findById(id).ifPresent(a -> {
            a.setDismissed(true);
            alertRepository.save(a);
        });
        return ResponseEntity.noContent().build();
    }

    // Turn flags for a session
    @GetMapping("/flags/session/{sessionId}")
    public ResponseEntity<List<TurnFlag>> getFlagsForSession(@PathVariable String sessionId) {
        return ResponseEntity.ok(turnFlagRepository.findBySessionId(sessionId));
    }

    // All active flags
    @GetMapping("/flags")
    public ResponseEntity<List<TurnFlag>> getActiveFlags() {
        return ResponseEntity.ok(turnFlagRepository.findByDismissedFalse());
    }

    // Dismiss flag
    @PostMapping("/flags/{id}/dismiss")
    public ResponseEntity<Void> dismissFlag(@PathVariable Long id) {
        turnFlagRepository.findById(id).ifPresent(f -> {
            f.setDismissed(true);
            turnFlagRepository.save(f);
        });
        return ResponseEntity.noContent().build();
    }

    // Alert count — for badge in UI
    @GetMapping("/alerts/count")
    public ResponseEntity<Long> getAlertCount() {
        return ResponseEntity.ok(alertRepository.countByDismissedFalse());
    }
}
