package com.llmscoring.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.llmscoring.dto.ScenarioDefinitionRequest;
import com.llmscoring.model.Scenario;
import com.llmscoring.service.ScenarioService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ScenarioController.class)
class ScenarioControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ScenarioService scenarioService;

    private Scenario mockScenario;

    @BeforeEach
    void setUp() {
        mockScenario = new Scenario();
        mockScenario.setId(1L);
        mockScenario.setName("return-agent");
        mockScenario.setDescription("Return flow test");
        mockScenario.setPolicy(List.of("Always collect order ID"));
        mockScenario.setExpectedBehaviors(List.of("Bot collected order ID"));
        mockScenario.setScorerNames(List.of("policyCompliance", "expectedBehavior"));
        mockScenario.setAlertThreshold(0.7);
        mockScenario.setActive(true);
    }

    private ScenarioDefinitionRequest buildValidRequest() {
        ScenarioDefinitionRequest request = new ScenarioDefinitionRequest();
        request.setName("return-agent");
        request.setPolicy(List.of("Always collect order ID"));
        request.setExpectedBehaviors(List.of("Bot collected order ID"));
        request.setScorerNames(List.of("policyCompliance"));
        return request;
    }

    @Test
    void create_shouldReturn200ForValidRequest() throws Exception {
        when(scenarioService.create(any())).thenReturn(mockScenario);

        mockMvc.perform(post("/api/v1/scenarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildValidRequest())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("return-agent"))
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    void create_shouldReturn400WhenNameIsBlank() throws Exception {
        ScenarioDefinitionRequest request = buildValidRequest();
        request.setName("");

        mockMvc.perform(post("/api/v1/scenarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"));
    }

    @Test
    void create_shouldReturn400WhenPolicyIsEmpty() throws Exception {
        ScenarioDefinitionRequest request = buildValidRequest();
        request.setPolicy(List.of());

        mockMvc.perform(post("/api/v1/scenarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void create_shouldReturn400ForDuplicateName() throws Exception {
        when(scenarioService.create(any()))
                .thenThrow(new IllegalArgumentException("Scenario already exists: return-agent"));

        mockMvc.perform(post("/api/v1/scenarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildValidRequest())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Scenario already exists: return-agent"));
    }

    @Test
    void getAll_shouldReturn200WithList() throws Exception {
        when(scenarioService.getAll()).thenReturn(List.of(mockScenario));

        mockMvc.perform(get("/api/v1/scenarios"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].name").value("return-agent"));
    }

    @Test
    void getByName_shouldReturn200ForExistingScenario() throws Exception {
        when(scenarioService.getByName("return-agent")).thenReturn(mockScenario);

        mockMvc.perform(get("/api/v1/scenarios/name/return-agent"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("return-agent"));
    }

    @Test
    void getByName_shouldReturn400ForUnknownScenario() throws Exception {
        when(scenarioService.getByName("unknown"))
                .thenThrow(new IllegalArgumentException("Scenario not found: unknown"));

        mockMvc.perform(get("/api/v1/scenarios/name/unknown"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Scenario not found: unknown"));
    }

    @Test
    void getAvailableScorers_shouldReturn200WithList() throws Exception {
        when(scenarioService.getAvailableScorers())
                .thenReturn(List.of("faithfulness", "hallucination", "policyCompliance"));

        mockMvc.perform(get("/api/v1/scenarios/scorers/available"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(3));
    }

    @Test
    void deactivate_shouldReturn204() throws Exception {
        mockMvc.perform(delete("/api/v1/scenarios/1"))
                .andExpect(status().isNoContent());
    }
}
