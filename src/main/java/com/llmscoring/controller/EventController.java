package com.llmscoring.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.llmscoring.config.KafkaConfig;
import com.llmscoring.dto.ConversationEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/events")
@RequiredArgsConstructor
public class EventController {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @PostMapping("/publish")
    public ResponseEntity<String> publish(@RequestBody ConversationEvent event) {
        try {
            if (event.getEventId() == null) {
                event.setEventId(UUID.randomUUID().toString());
            }
            if (event.getTimestamp() == null) {
                event.setTimestamp(Instant.now());
            }

            String payload = objectMapper.writeValueAsString(event);
            kafkaTemplate.send(KafkaConfig.CONVERSATION_EVENTS_TOPIC,
                    event.getSessionId(), payload);

            log.info("Published event — session={} scenario={}",
                    event.getSessionId(), event.getScenarioName());

            return ResponseEntity.ok("Event published: " + event.getEventId());
        } catch (Exception e) {
            log.error("Failed to publish event", e);
            return ResponseEntity.internalServerError()
                    .body("Failed: " + e.getMessage());
        }
    }
}
