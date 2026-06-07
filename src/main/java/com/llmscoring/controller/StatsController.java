package com.llmscoring.controller;

import com.llmscoring.dto.stats.*;
import com.llmscoring.service.StatsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/stats")
@RequiredArgsConstructor
public class StatsController {

    private final StatsService statsService;

    @GetMapping("/overview")
    public ResponseEntity<OverviewStats> getOverview() {
        return ResponseEntity.ok(statsService.getOverview());
    }

    @GetMapping("/by-model")
    public ResponseEntity<List<ModelStats>> getByModel() {
        return ResponseEntity.ok(statsService.getByModel());
    }

    @GetMapping("/trends")
    public ResponseEntity<List<TrendPoint>> getTrends(
            @RequestParam(defaultValue = "30") int days) {
        return ResponseEntity.ok(statsService.getTrends(days));
    }

    @GetMapping("/failures")
    public ResponseEntity<List<FailureStats>> getFailures() {
        return ResponseEntity.ok(statsService.getFailureStats());
    }
}
