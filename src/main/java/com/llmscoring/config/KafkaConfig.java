package com.llmscoring.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaConfig {

    public static final String CONVERSATION_EVENTS_TOPIC = "conversation-events";
    public static final String SCORING_RESULTS_TOPIC = "scoring-results";

    @Bean
    public NewTopic conversationEventsTopic() {
        return TopicBuilder.name(CONVERSATION_EVENTS_TOPIC)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic scoringResultsTopic() {
        return TopicBuilder.name(SCORING_RESULTS_TOPIC)
                .partitions(3)
                .replicas(1)
                .build();
    }
}
