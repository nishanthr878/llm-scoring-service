package com.llmscoring.transformer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.llmscoring.dto.ChatMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class HumanAITransformerTest {

    private HumanAITransformer transformer;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        transformer = new HumanAITransformer();
        objectMapper = new ObjectMapper();
    }

    @Test
    void formatName_shouldReturnHumanAi() {
        assertThat(transformer.formatName()).isEqualTo("human-ai");
    }

    @Test
    void transform_shouldMapHumanToUser() throws Exception {
        JsonNode messages = objectMapper.readTree("""
                [
                  {"speaker": "human", "text": "Hello"},
                  {"speaker": "ai", "text": "Hi there"}
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
    void transform_shouldBeCaseInsensitive() throws Exception {
        JsonNode messages = objectMapper.readTree("""
                [
                  {"speaker": "HUMAN", "text": "Hello"},
                  {"speaker": "AI", "text": "Hi"}
                ]
                """);

        List<ChatMessage> result = transformer.transform(messages);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getRole()).isEqualTo("user");
        assertThat(result.get(1).getRole()).isEqualTo("assistant");
    }

    @Test
    void transform_shouldSkipMessagesWithBlankText() throws Exception {
        JsonNode messages = objectMapper.readTree("""
                [
                  {"speaker": "human", "text": ""},
                  {"speaker": "ai", "text": "Hi"}
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
}
