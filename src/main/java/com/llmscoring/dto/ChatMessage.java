package com.llmscoring.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class ChatMessage {

    @Pattern(regexp = "user|assistant", message = "Role must be 'user' or 'assistant'")
    private String role;

    @NotBlank(message = "Content must not be blank")
    private String content;
}
