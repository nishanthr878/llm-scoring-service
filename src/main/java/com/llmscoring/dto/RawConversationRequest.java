package com.llmscoring.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class RawConversationRequest {

    @NotBlank(message = "Raw log must not be blank")
    private String rawLog;

    @NotEmpty(message = "At least one context chunk is required")
    private List<String> retrievedChunks;

    private String sessionId;
    private String modelName;
    private String promptVersion;
}
