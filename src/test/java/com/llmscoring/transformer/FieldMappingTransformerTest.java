package com.llmscoring.transformer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.llmscoring.dto.ChatMessage;
import com.llmscoring.dto.FieldMapping;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class FieldMappingTransformerTest {

    private FieldMappingTransformer transformer;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        transformer = new FieldMappingTransformer();
        objectMapper = new ObjectMapper();
    }

    @Test
    void formatName_shouldReturnFieldMapping() {
        assertThat(transformer.formatName()).isEqualTo("field-mapping");
    }

    @Test
    void transform_shouldUseCustomFieldNames() throws Exception {
        FieldMapping mapping = new FieldMapping();
        mapping.setRoleField("sender");
        mapping.setUserRoleValue("customer");
        mapping.setAssistantRoleValue("agent");
        mapping.setContentField("body");

        JsonNode messages = objectMapper.readTree("""
                [
                  {"sender": "customer", "body": "I need help"},
                  {"sender": "agent", "body": "Sure, how can I help?"}
                ]
                """);

        List<ChatMessage> result = transformer.transform(messages, mapping);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getRole()).isEqualTo("user");
        assertThat(result.get(0).getContent()).isEqualTo("I need help");
        assertThat(result.get(1).getRole()).isEqualTo("assistant");
        assertThat(result.get(1).getContent()).isEqualTo("Sure, how can I help?");
    }

    @Test
    void transform_shouldUseDefaultMappingWhenNotProvided() throws Exception {
        JsonNode messages = objectMapper.readTree("""
                [
                  {"role": "user", "content": "Hello"},
                  {"role": "assistant", "content": "Hi"}
                ]
                """);

        // Default mapping uses role/content fields
        FieldMapping defaultMapping = new FieldMapping();
        List<ChatMessage> result = transformer.transform(messages, defaultMapping);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getRole()).isEqualTo("user");
        assertThat(result.get(1).getRole()).isEqualTo("assistant");
    }

    @Test
    void transform_shouldSkipBlankContent() throws Exception {
        FieldMapping mapping = new FieldMapping();
        mapping.setRoleField("sender");
        mapping.setUserRoleValue("customer");
        mapping.setContentField("body");

        JsonNode messages = objectMapper.readTree("""
                [
                  {"sender": "customer", "body": ""},
                  {"sender": "customer", "body": "Hello"}
                ]
                """);

        List<ChatMessage> result = transformer.transform(messages, mapping);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getContent()).isEqualTo("Hello");
    }

    @Test
    void transform_shouldReturnEmptyForEmptyArray() throws Exception {
        JsonNode messages = objectMapper.readTree("[]");
        List<ChatMessage> result = transformer.transform(messages, new FieldMapping());
        assertThat(result).isEmpty();
    }
}
