package com.llmscoring.service;

import com.llmscoring.dto.ScenarioDefinitionRequest;
import com.llmscoring.model.Scenario;
import com.llmscoring.repository.ScenarioRepository;
import com.llmscoring.scorer.ScorerRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ScenarioService {

    private final ScenarioRepository scenarioRepository;
    private final ScorerRegistry scorerRegistry;

    public Scenario create(ScenarioDefinitionRequest request) {
        if (scenarioRepository.existsByName(request.getName())) {
            throw new IllegalArgumentException(
                    "Scenario already exists: " + request.getName());
        }

        // Validate scorer names exist in registry
        validateScorerNames(request.getScorerNames());

        Scenario scenario = new Scenario();
        scenario.setName(request.getName());
        scenario.setDescription(request.getDescription());
        scenario.setPolicy(request.getPolicy());
        scenario.setExpectedBehaviors(request.getExpectedBehaviors());
        scenario.setScorerNames(request.getScorerNames());
        scenario.setAlertThreshold(request.getAlertThreshold());

        Scenario saved = scenarioRepository.save(scenario);
        log.info("Scenario created — name={}", saved.getName());
        return saved;
    }

    public Scenario update(Long id, ScenarioDefinitionRequest request) {
        Scenario scenario = getById(id);
        validateScorerNames(request.getScorerNames());

        scenario.setName(request.getName());
        scenario.setDescription(request.getDescription());
        scenario.setPolicy(request.getPolicy());
        scenario.setExpectedBehaviors(request.getExpectedBehaviors());
        scenario.setScorerNames(request.getScorerNames());
        scenario.setAlertThreshold(request.getAlertThreshold());

        return scenarioRepository.save(scenario);
    }

    public Scenario getById(Long id) {
        return scenarioRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Scenario not found: " + id));
    }

    public Scenario getByName(String name) {
        return scenarioRepository.findByName(name)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Scenario not found: " + name));
    }

    public List<Scenario> getAll() {
        return scenarioRepository.findAll();
    }

    public List<Scenario> getActive() {
        return scenarioRepository.findByActiveTrue();
    }

    public void deactivate(Long id) {
        Scenario scenario = getById(id);
        scenario.setActive(false);
        scenarioRepository.save(scenario);
        log.info("Scenario deactivated — name={}", scenario.getName());
    }

    public List<String> getAvailableScorers() {
        return scorerRegistry.getAll()
                .stream()
                .map(s -> s.name())
                .toList();
    }

    private void validateScorerNames(List<String> scorerNames) {
        List<String> available = scorerRegistry.getAll()
                .stream()
                .map(s -> s.name())
                .toList();

        List<String> invalid = scorerNames.stream()
                .filter(name -> !available.contains(name))
                .toList();

        if (!invalid.isEmpty()) {
            throw new IllegalArgumentException(
                    "Unknown scorers: " + invalid +
                    ". Available: " + available);
        }
    }
}
