package com.llmscoring.transformer;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class TransformerRegistry {

    private final Map<String, MessageTransformer> transformers;

    public TransformerRegistry(List<MessageTransformer> transformerList) {
        this.transformers = transformerList.stream()
                .collect(Collectors.toMap(
                        MessageTransformer::formatName,
                        Function.identity()
                ));
    }

    public MessageTransformer get(String formatName) {
        MessageTransformer transformer = transformers.get(
                formatName == null ? "openai" : formatName.toLowerCase());

        if (transformer == null) {
            throw new IllegalArgumentException(
                    "Unknown format: " + formatName +
                    ". Available: " + transformers.keySet());
        }

        return transformer;
    }

    public List<String> getAvailableFormats() {
        return List.copyOf(transformers.keySet());
    }
}
