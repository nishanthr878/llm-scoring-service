package com.llmscoring.scorer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Slf4j
@Component
@RequiredArgsConstructor
public class ExpectedBehaviorScorer implements Scorer {

    private final ChatClient chatClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String PROMPT_TEMPLATE = """
            You are an expert evaluator checking if an AI assistant exhibited expected behaviors.
            
            EXPECTED BEHAVIORS:
            %s
            
            CONVERSATION:
            %s
            
            For each expected behavior, determine if the assistant exhibited it in the conversation.
            
            Respond ONLY in this exact JSON format, nothing else:
            {
              "overallScore": <float between 0.0 and 1.0>,
              "results": [
                {
                  "behavior": "<exact behavior text>",
                  "passed": <true or false>,
                  "reasoning": "<one sentence explanation>"
                }
              ]
            }
            
            overallScore = number of passed behaviors / total behaviors.
            """;

    @Override
    public String name() {
        return "expectedBehavior";
    }

    @Override
    public ScorerResult score(Map<String, Object> context) {
        List<String> behaviors = (List<String>) context.get("expectedBehaviors");
        String conversation = (String) context.get("formattedConversation");

        if (behaviors == null || behaviors.isEmpty()) {
            return ScorerResult.error("No expected behaviors provided");
        }

        String formattedBehaviors = IntStream.range(0, behaviors.size())
                .mapToObj(i -> (i + 1) + ". " + behaviors.get(i))
                .collect(Collectors.joining("\n"));

        String prompt = PROMPT_TEMPLATE.formatted(formattedBehaviors, conversation);

        try {
            String response = chatClient.prompt()
                    .user(prompt)
                    .call()
                    .content();

            return parseDetailedResponse(response);
        } catch (Exception e) {
            log.error("ExpectedBehaviorScorer failed", e);
            return ScorerResult.error("ExpectedBehaviorScorer failed: " + e.getMessage());
        }
    }

    private ScorerResult parseDetailedResponse(String response) {
        try {
            String cleaned = response
                    .replaceAll("```json", "")
                    .replaceAll("```", "")
                    .trim();

            JsonNode root = objectMapper.readTree(cleaned);
            double overallScore = root.get("overallScore").asDouble();

            List<Map<String, Object>> details = new ArrayList<>();
            JsonNode results = root.get("results");

            long passedCount = 0;
            StringBuilder reasoningSummary = new StringBuilder();

            for (JsonNode result : results) {
                Map<String, Object> detail = new HashMap<>();
                String behavior = result.get("behavior").asText();
                boolean passed = result.get("passed").asBoolean();
                String reasoning = result.get("reasoning").asText();

                detail.put("behavior", behavior);
                detail.put("passed", passed);
                detail.put("reasoning", reasoning);
                details.add(detail);

                if (passed) passedCount++;
                if (!passed) {
                    reasoningSummary.append("FAILED: ")
                            .append(behavior)
                            .append(" — ")
                            .append(reasoning)
                            .append(". ");
                }
            }

            String summary = reasoningSummary.isEmpty()
                    ? "All expected behaviors observed."
                    : reasoningSummary.toString().trim();

            return ScorerResult.withDetails(overallScore, summary, details);

        } catch (Exception e) {
            log.warn("ExpectedBehaviorScorer failed to parse response: {}", response);
            return ScorerResult.error("Parse failed: " + response);
        }
    }
}
