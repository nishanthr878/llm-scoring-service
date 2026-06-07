package com.llmscoring.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.llmscoring.config.KafkaConfig;
import com.llmscoring.dto.ConversationEvent;
import com.llmscoring.dto.ConversationRequest;
import com.llmscoring.dto.ScenarioEvaluationRequest;
import com.llmscoring.model.ScoringResult;
import com.llmscoring.service.ScoringService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ConversationEventConsumer {

    private final ScoringService scoringService;
    private final ObjectMapper objectMapper;

    @KafkaListener(
            topics = KafkaConfig.CONVERSATION_EVENTS_TOPIC,
            groupId = "llm-scoring-group"
    )
    public void consume(String message) {
        try {
            ConversationEvent event = objectMapper.readValue(message, ConversationEvent.class);
            log.info("Received conversation event — session={} scenario={}",
                    event.getSessionId(), event.getScenarioName());

            ScoringResult result = route(event);

            log.info("Auto-scored event — session={} overall={}",
                    event.getSessionId(), result.getOverallPassed());

        } catch (Exception e) {
            log.error("Failed to process conversation event: {}", message, e);
        }
    }

    private ScoringResult route(ConversationEvent event) {
        // If scenario name provided — use scenario registry
        if (event.getScenarioName() != null && !event.getScenarioName().isBlank()) {
            ScenarioEvaluationRequest request = new ScenarioEvaluationRequest();
            request.setMessages(event.getMessages());
            request.setSessionId(event.getSessionId());
            request.setModelName(event.getModelName());
            return scoringService.evaluateWithScenario(event.getScenarioName(), request);
        }

        // Otherwise run standard conversation scoring
        ConversationRequest request = new ConversationRequest();
        request.setMessages(event.getMessages());
        request.setRetrievedChunks(event.getRetrievedChunks());
        request.setSessionId(event.getSessionId());
        request.setModelName(event.getModelName());
        return scoringService.evaluateConversation(request);
    }
}
