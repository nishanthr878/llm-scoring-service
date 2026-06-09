package com.llmscoring.scorer;

import com.llmscoring.dto.ChatMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Slf4j
@Component
@RequiredArgsConstructor
public class TurnHallucinationScorer extends BaseScorer implements Scorer {

    private final ChatClient chatClient;

    private static final String PROMPT_TEMPLATE = """
            You are an expert evaluator detecting hallucinations in AI conversations.
            
            CONTEXT DOCUMENTS:
            %s
            
            CONVERSATION:
            %s
            
            For each ASSISTANT turn, evaluate if it contains hallucinations.
            Score 0.0 = no hallucination, 1.0 = severe hallucination.
            Only evaluate assistant turns.
            
            Respond ONLY in this exact JSON format, nothing else:
            {
              "turns": [
                {
                  "turnIndex": <index of assistant turn>,
                  "score": <float 0.0-1.0>,
                  "reasoning": "<one sentence>"
                }
              ]
            }
            """;

    @Override
    public String name() {
        return "turnHallucination";
    }

    @Override
    public ScorerResult score(Map<String, Object> context) {
        List<ChatMessage> messages = (List<ChatMessage>) context.get("messages");
        List<String> chunks = (List<String>) context.getOrDefault(
                "retrievedChunks", List.of());

        if (messages == null || messages.isEmpty()) {
            return new ScorerResult(1.0, "No messages to evaluate", true);
        }

        String formattedConversation = IntStream.range(0, messages.size())
                .mapToObj(i -> String.format("[Turn %d] %s: %s",
                        i, messages.get(i).getRole().toUpperCase(),
                        messages.get(i).getContent()))
                .collect(Collectors.joining("\n"));

        String formattedChunks = chunks.isEmpty()
                ? "No context provided"
                : String.join("\n---\n", chunks);

        String prompt = PROMPT_TEMPLATE.formatted(formattedChunks, formattedConversation);

        try {
            String response = callWithRetry(chatClient, prompt, name());
            return parseTurnResponse(response, messages);
        } catch (Exception e) {
            log.error("TurnHallucinationScorer failed", e);
            return ScorerResult.error("TurnHallucinationScorer failed: " + e.getMessage());
        }
    }

    private ScorerResult parseTurnResponse(String response, List<ChatMessage> messages) {
        try {
            String cleaned = response
                    .replaceAll("```json", "")
                    .replaceAll("```", "")
                    .trim();

            // Parse turns array
            int turnsIdx = cleaned.indexOf("\"turns\"");
            int arrStart = cleaned.indexOf("[", turnsIdx);
            int arrEnd = cleaned.lastIndexOf("]");
            String turnsJson = cleaned.substring(arrStart, arrEnd + 1);

            List<Map<String, Object>> turnResults = new ArrayList<>();
            double maxScore = 0.0;
            int worstTurn = -1;
            List<String> flaggedReasons = new ArrayList<>();

            // Simple parse each turn object
            String[] turnObjects = turnsJson.split("\\},\\s*\\{");
            for (String obj : turnObjects) {
                obj = obj.replaceAll("[\\[\\]\\{\\}]", "").trim();

                int turnIndex = extractInt(obj, "turnIndex");
                double score = extractDouble(obj, "score");
                String reasoning = extractString(obj, "reasoning");

                Map<String, Object> turn = new HashMap<>();
                turn.put("turnIndex", turnIndex);
                turn.put("score", score);
                turn.put("reasoning", reasoning);
                turn.put("role", "assistant");

                if (turnIndex >= 0 && turnIndex < messages.size()) {
                    turn.put("content", messages.get(turnIndex).getContent());
                }

                turnResults.add(turn);

                if (score > maxScore) {
                    maxScore = score;
                    worstTurn = turnIndex;
                }

                if (score >= 0.7) {
                    flaggedReasons.add(String.format("Turn %d: %s", turnIndex, reasoning));
                }
            }

            String summary = flaggedReasons.isEmpty()
                    ? "No turn-level hallucinations detected"
                    : String.format("Hallucination detected at turn(s): %s",
                      String.join("; ", flaggedReasons));

            return ScorerResult.withDetails(maxScore, summary, turnResults);

        } catch (Exception e) {
            log.warn("TurnHallucinationScorer parse failed: {}", response);
            return ScorerResult.error("Parse failed: " + response);
        }
    }

    private int extractInt(String obj, String key) {
        try {
            int idx = obj.indexOf("\"" + key + "\"");
            String after = obj.substring(idx + key.length() + 3).trim();
            String num = after.split("[,}\\s]")[0].trim();
            return Integer.parseInt(num);
        } catch (Exception e) { return -1; }
    }

    private double extractDouble(String obj, String key) {
        try {
            int idx = obj.indexOf("\"" + key + "\"");
            String after = obj.substring(idx + key.length() + 3).trim();
            String num = after.split("[,}\\s]")[0].trim();
            return Double.parseDouble(num);
        } catch (Exception e) { return 0.0; }
    }

    private String extractString(String obj, String key) {
        try {
            int idx = obj.indexOf("\"" + key + "\"");
            int start = obj.indexOf("\"", idx + key.length() + 3) + 1;
            int end = obj.indexOf("\"", start);
            return obj.substring(start, end);
        } catch (Exception e) { return ""; }
    }
}
