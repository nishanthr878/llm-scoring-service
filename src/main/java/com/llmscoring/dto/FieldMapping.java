package com.llmscoring.dto;

import lombok.Data;

@Data
public class FieldMapping {

    // Which field contains the role/speaker
    private String roleField = "role";

    // Which value means "user"
    private String userRoleValue = "user";

    // Which value means "assistant"
    private String assistantRoleValue = "assistant";

    // Which field contains the message content
    private String contentField = "content";
}
