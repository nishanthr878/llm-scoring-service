package com.llmscoring.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.llmscoring.dto.TraceRequest;
import com.llmscoring.enums.ScoringType;
import com.llmscoring.model.ScoringResult;
import com.llmscoring.service.ScoringService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ScoringController.class)
class ScoringControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ScoringService scoringService;

    private ScoringResult mockResult;

    @BeforeEach
    void setUp() {
        mockResult = new ScoringResult();
        mockResult.setId(1L);
        mockResult.setSessionId("test-session");
        mockResult.setModelName("test-model");
        mockResult.setType(ScoringType.SINGLE_TURN);
        mockResult.setOverallPassed(true);
        mockResult.setScores(Map.of("faithfulness", 0.9));
        mockResult.setReasoning(Map.of("faithfulness", "Faithful"));
        mockResult.setPassed(Map.of("faithfulness", true));
        mockResult.setScoredAt(Instant.now());
    }

    private TraceRequest buildValidRequest() {
        TraceRequest request = new TraceRequest();
        request.setQuestion("What is the capital of France?");
        request.setAnswer("Paris");
        request.setRetrievedChunks(List.of("Paris is the capital of France."));
        request.setSessionId("test-session");
        request.setModelName("test-model");
        return request;
    }

    @Test
    void evaluate_shouldReturn200ForValidRequest() throws Exception {
        when(scoringService.evaluate(any())).thenReturn(mockResult);

        mockMvc.perform(post("/api/v1/scoring/evaluate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildValidRequest())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sessionId").value("test-session"))
                .andExpect(jsonPath("$.overallPassed").value(true))
                .andExpect(jsonPath("$.type").value("SINGLE_TURN"));
    }

    @Test
    void evaluate_shouldReturn400WhenQuestionIsBlank() throws Exception {
        TraceRequest request = buildValidRequest();
        request.setQuestion("");

        mockMvc.perform(post("/api/v1/scoring/evaluate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.errors").isArray());
    }

    @Test
    void evaluate_shouldReturn400WhenAnswerIsBlank() throws Exception {
        TraceRequest request = buildValidRequest();
        request.setAnswer("");

        mockMvc.perform(post("/api/v1/scoring/evaluate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void evaluate_shouldReturn400WhenChunksAreEmpty() throws Exception {
        TraceRequest request = buildValidRequest();
        request.setRetrievedChunks(List.of());

        mockMvc.perform(post("/api/v1/scoring/evaluate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getAll_shouldReturn200WithList() throws Exception {
        when(scoringService.getAll()).thenReturn(List.of(mockResult));

        mockMvc.perform(get("/api/v1/scoring/results"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].sessionId").value("test-session"));
    }

    @Test
    void getFailed_shouldReturn200WithFailedResults() throws Exception {
        mockResult.setOverallPassed(false);
        when(scoringService.getFailed()).thenReturn(List.of(mockResult));

        mockMvc.perform(get("/api/v1/scoring/results/failed"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].overallPassed").value(false));
    }

    @Test
    void getById_shouldReturn200ForExistingId() throws Exception {
        when(scoringService.getById(1L)).thenReturn(mockResult);

        mockMvc.perform(get("/api/v1/scoring/results/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void getById_shouldReturn500ForNonExistingId() throws Exception {
        when(scoringService.getById(999L))
                .thenThrow(new RuntimeException("ScoringResult not found: 999"));

        mockMvc.perform(get("/api/v1/scoring/results/999"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("ScoringResult not found: 999"));
    }

    @Test
    void getBySession_shouldReturn200WithSessionResults() throws Exception {
        when(scoringService.getBySession("test-session"))
                .thenReturn(List.of(mockResult));

        mockMvc.perform(get("/api/v1/scoring/results/session/test-session"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].sessionId").value("test-session"));
    }
}
