package com.llmscoring.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.llmscoring.config.KafkaConfig;
import com.llmscoring.dto.*;
import com.llmscoring.transformer.FieldMappingTransformer;
import com.llmscoring.transformer.MessageTransformer;
import com.llmscoring.transformer.TransformerRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class IngestService {

    private final TransformerRegistry transformerRegistry;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public String ingest(IngestRequest request) {
        // 1. Get the right transformer
        MessageTransformer transformer = resolveTransformer(request);

        // 2. Validate format is supported
        if (!transformer.supports(request.getMessages())) {
            throw new IllegalArgumentException(
                    "Messages format not supported by transformer: " + request.getFormat());
        }

        // 3. Transform to our ChatMessage format
        List<ChatMessage> messages = transform(transformer, request);

        if (messages.isEmpty()) {
            throw new IllegalArgumentException(
                    "No valid messages after transformation. " +
                    "Check your format and field mapping.");
        }

        // 4. Build conversation event
        ConversationEvent event = buildEvent(request, messages);

        // 5. Publish to Kafka async
        publishToKafka(event);

        log.info("Ingested event — session={} format={} messages={} scenario={}",
                request.getSessionId(),
                request.getFormat(),
                messages.size(),
                request.getScenarioName());

        return event.getEventId();
    }

    private MessageTransformer resolveTransformer(IngestRequest request) {
        String format = request.getFormat() == null ? "openai" : request.getFormat();

        MessageTransformer transformer = transformerRegistry.get(format);

        // For field-mapping format, inject the mapping from request
        if (transformer instanceof FieldMappingTransformer && request.getFieldMapping() != null) {
            return new FieldMappingTransformerWrapper(
                    (FieldMappingTransformer) transformer,
                    request.getFieldMapping()
            );
        }

        return transformer;
    }

    private List<ChatMessage> transform(MessageTransformer transformer,
                                         IngestRequest request) {
        if (transformer instanceof FieldMappingTransformerWrapper wrapper) {
            return wrapper.transform(request.getMessages());
        }
        return transformer.transform(request.getMessages());
    }

    private ConversationEvent buildEvent(IngestRequest request,
                                          List<ChatMessage> messages) {
        ConversationEvent event = new ConversationEvent();
        event.setEventId(UUID.randomUUID().toString());
        event.setSessionId(request.getSessionId());
        event.setScenarioName(request.getScenarioName());
        event.setModelName(request.getModelName());
        event.setMessages(messages);
        event.setTimestamp(Instant.now());

        // Transform retrieved chunks if present
        if (request.getRetrievedChunks() != null) {
            List<String> chunks = new ArrayList<>();
            for (JsonNode chunk : request.getRetrievedChunks()) {
                chunks.add(chunk.asText());
            }
            event.setRetrievedChunks(chunks);
        }

        return event;
    }

    private void publishToKafka(ConversationEvent event) {
        try {
            String payload = objectMapper.writeValueAsString(event);
            kafkaTemplate.send(KafkaConfig.CONVERSATION_EVENTS_TOPIC,
                    event.getSessionId(), payload);
        } catch (Exception e) {
            log.error("Failed to publish to Kafka", e);
            throw new RuntimeException("Failed to publish event to Kafka", e);
        }
    }

    // Wrapper to inject field mapping into transformer
    private record FieldMappingTransformerWrapper(
            FieldMappingTransformer transformer,
            FieldMapping mapping) implements MessageTransformer {

        @Override
        public String formatName() {
            return "field-mapping";
        }

        @Override
        public List<ChatMessage> transform(JsonNode messagesNode) {
            return transformer.transform(messagesNode, mapping);
        }
    }
}
