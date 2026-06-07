package com.llmscoring.dto;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class IngestRequest {

    @NotBlank(message = "sessionId must not be blank")
    private String sessionId;

    // Optional — if provided, use scenario registry
    private String scenarioName;

    // Format of the messages — defaults to "openai"
    private String format;

    // Field mapping — only used when format is "field-mapping"
    private FieldMapping fieldMapping;

    // Raw messages in whatever format the caller uses
    @NotNull(message = "messages must not be null")
    private JsonNode messages;

    // Optional context chunks for non-scenario evaluations
    private JsonNode retrievedChunks;

    private String modelName;
    private String promptVersion;
}
