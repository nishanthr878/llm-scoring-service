package com.llmscoring.scorer;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class ScorerRegistry {

    private final Map<String, Scorer> scorers;

    // Spring injects all Scorer beans automatically
    public ScorerRegistry(List<Scorer> scorerList) {
        this.scorers = scorerList.stream()
                .collect(Collectors.toMap(Scorer::name, Function.identity()));
    }

    public Scorer get(String name) {
        Scorer scorer = scorers.get(name);
        if (scorer == null) {
            throw new IllegalArgumentException("Unknown scorer: " + name);
        }
        return scorer;
    }

    public List<Scorer> getAll() {
        return List.copyOf(scorers.values());
    }

    public List<Scorer> get(List<String> names) {
        return names.stream().map(this::get).toList();
    }
}
