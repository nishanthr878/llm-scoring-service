package com.llmscoring.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class ScenarioDefinitionRequest {

    @NotBlank(message = "Scenario name must not be blank")
    private String name;

    private String description;

    @NotEmpty(message = "Policy rules must not be empty")
    private List<String> policy;

    @NotEmpty(message = "Expected behaviors must not be empty")
    private List<String> expectedBehaviors;

    @NotEmpty(message = "Scorer names must not be empty")
    private List<String> scorerNames;

    private Double alertThreshold;
}
