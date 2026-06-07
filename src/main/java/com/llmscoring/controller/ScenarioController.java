package com.llmscoring.controller;

import com.llmscoring.dto.ScenarioDefinitionRequest;
import com.llmscoring.model.Scenario;
import com.llmscoring.service.ScenarioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/scenarios")
@RequiredArgsConstructor
public class ScenarioController {

    private final ScenarioService scenarioService;

    // Create a new scenario
    @PostMapping
    public ResponseEntity<Scenario> create(
            @Valid @RequestBody ScenarioDefinitionRequest request) {
        return ResponseEntity.ok(scenarioService.create(request));
    }

    // Update existing scenario
    @PutMapping("/{id}")
    public ResponseEntity<Scenario> update(
            @PathVariable Long id,
            @Valid @RequestBody ScenarioDefinitionRequest request) {
        return ResponseEntity.ok(scenarioService.update(id, request));
    }

    // Get all scenarios
    @GetMapping
    public ResponseEntity<List<Scenario>> getAll() {
        return ResponseEntity.ok(scenarioService.getAll());
    }

    // Get active scenarios only
    @GetMapping("/active")
    public ResponseEntity<List<Scenario>> getActive() {
        return ResponseEntity.ok(scenarioService.getActive());
    }

    // Get scenario by id
    @GetMapping("/{id}")
    public ResponseEntity<Scenario> getById(@PathVariable Long id) {
        return ResponseEntity.ok(scenarioService.getById(id));
    }

    // Get scenario by name
    @GetMapping("/name/{name}")
    public ResponseEntity<Scenario> getByName(@PathVariable String name) {
        return ResponseEntity.ok(scenarioService.getByName(name));
    }

    // Deactivate a scenario
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deactivate(@PathVariable Long id) {
        scenarioService.deactivate(id);
        return ResponseEntity.noContent().build();
    }

    // List all available scorers — useful for UI dropdown
    @GetMapping("/scorers/available")
    public ResponseEntity<List<String>> getAvailableScorers() {
        return ResponseEntity.ok(scenarioService.getAvailableScorers());
    }
}
