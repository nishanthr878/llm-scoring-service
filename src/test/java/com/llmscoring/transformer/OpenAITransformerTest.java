package com.llmscoring.transformer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.llmscoring.dto.ChatMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class OpenAITransformerTest {

    private OpenAITransformer transformer;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        transformer = new OpenAITransformer();
        objectMapper = new ObjectMapper();
    }

    @Test
    void formatName_shouldReturnOpenai() {
        assertThat(transformer.formatName()).isEqualTo("openai");
    }

    @Test
    void transform_shouldConvertStandardRoles() throws Exception {
        JsonNode messages = objectMapper.readTree("""
                [
                  {"role": "user", "content": "Hello"},
                  {"role": "assistant", "content": "Hi there"}
                ]
                """);

        List<ChatMessage> result = transformer.transform(messages);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getRole()).isEqualTo("user");
        assertThat(result.get(0).getContent()).isEqualTo("Hello");
        assertThat(result.get(1).getRole()).isEqualTo("assistant");
        assertThat(result.get(1).getContent()).isEqualTo("Hi there");
    }

    @Test
    void transform_shouldNormalizeHumanToUser() throws Exception {
        JsonNode messages = objectMapper.readTree("""
                [
                  {"role": "human", "content": "Hello"},
                  {"role": "bot", "content": "Hi"}
                ]
                """);

        List<ChatMessage> result = transformer.transform(messages);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getRole()).isEqualTo("user");
        assertThat(result.get(1).getRole()).isEqualTo("assistant");
    }

    @Test
    void transform_shouldNormalizeCustomerAndAgent() throws Exception {
        JsonNode messages = objectMapper.readTree("""
                [
                  {"role": "customer", "content": "I need help"},
                  {"role": "agent", "content": "Sure"}
                ]
                """);

        List<ChatMessage> result = transformer.transform(messages);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getRole()).isEqualTo("user");
        assertThat(result.get(1).getRole()).isEqualTo("assistant");
    }

    @Test
    void transform_shouldSkipSystemMessages() throws Exception {
        JsonNode messages = objectMapper.readTree("""
                [
                  {"role": "system", "content": "You are a helpful assistant"},
                  {"role": "user", "content": "Hello"},
                  {"role": "assistant", "content": "Hi"}
                ]
                """);

        List<ChatMessage> result = transformer.transform(messages);

        // system message skipped
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getRole()).isEqualTo("user");
    }

    @Test
    void transform_shouldSkipMessagesWithBlankContent() throws Exception {
        JsonNode messages = objectMapper.readTree("""
                [
                  {"role": "user", "content": ""},
                  {"role": "assistant", "content": "Hi"}
                ]
                """);

        List<ChatMessage> result = transformer.transform(messages);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getRole()).isEqualTo("assistant");
    }

    @Test
    void transform_shouldReturnEmptyListForEmptyArray() throws Exception {
        JsonNode messages = objectMapper.readTree("[]");
        List<ChatMessage> result = transformer.transform(messages);
        assertThat(result).isEmpty();
    }

    @Test
    void supports_shouldReturnTrueForArray() throws Exception {
        JsonNode messages = objectMapper.readTree("[{\"role\": \"user\", \"content\": \"hi\"}]");
        assertThat(transformer.supports(messages)).isTrue();
    }

    @Test
    void supports_shouldReturnFalseForNull() {
        assertThat(transformer.supports(null)).isFalse();
    }

    @Test
    void supports_shouldReturnFalseForNonArray() throws Exception {
        JsonNode messages = objectMapper.readTree("{\"role\": \"user\"}");
        assertThat(transformer.supports(messages)).isFalse();
    }
}
