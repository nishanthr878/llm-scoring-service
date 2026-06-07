package com.llmscoring.transformer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TransformerRegistryTest {

    private TransformerRegistry registry;

    @BeforeEach
    void setUp() {
        List<MessageTransformer> transformers = List.of(
                new OpenAITransformer(),
                new HumanAITransformer(),
                new FieldMappingTransformer()
        );
        registry = new TransformerRegistry(transformers);
    }

    @Test
    void get_shouldReturnOpenAITransformerByDefault() {
        MessageTransformer transformer = registry.get("openai");
        assertThat(transformer).isInstanceOf(OpenAITransformer.class);
    }

    @Test
    void get_shouldReturnHumanAITransformer() {
        MessageTransformer transformer = registry.get("human-ai");
        assertThat(transformer).isInstanceOf(HumanAITransformer.class);
    }

    @Test
    void get_shouldReturnFieldMappingTransformer() {
        MessageTransformer transformer = registry.get("field-mapping");
        assertThat(transformer).isInstanceOf(FieldMappingTransformer.class);
    }

    @Test
    void get_shouldDefaultToOpenAIWhenNullPassed() {
        MessageTransformer transformer = registry.get(null);
        assertThat(transformer).isInstanceOf(OpenAITransformer.class);
    }

    @Test
    void get_shouldThrowForUnknownFormat() {
        assertThatThrownBy(() -> registry.get("unknown-format"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unknown format: unknown-format");
    }

    @Test
    void getAvailableFormats_shouldReturnAllFormats() {
        List<String> formats = registry.getAvailableFormats();
        assertThat(formats).containsExactlyInAnyOrder(
                "openai", "human-ai", "field-mapping");
    }
}
