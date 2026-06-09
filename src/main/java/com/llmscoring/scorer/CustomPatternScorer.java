package com.llmscoring.scorer;

import com.llmscoring.dto.ChatMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Pattern;

@Slf4j
@Component
public class CustomPatternScorer implements Scorer {

    @Override
    public String name() {
        return "customPattern";
    }

    @Override
    public ScorerResult score(Map<String, Object> context) {
        List<ChatMessage> messages = (List<ChatMessage>) context.get("messages");
        List<Map<String, String>> patterns =
                (List<Map<String, String>>) context.get("customPatterns");

        if (patterns == null || patterns.isEmpty()) {
            return new ScorerResult(1.0, "No custom patterns configured", true);
        }

        if (messages == null || messages.isEmpty()) {
            return new ScorerResult(1.0, "No messages to scan", true);
        }

        List<Map<String, Object>> matches = new ArrayList<>();

        for (int i = 0; i < messages.size(); i++) {
            ChatMessage msg = messages.get(i);

            for (Map<String, String> patternConfig : patterns) {
                String patternName = patternConfig.get("name");
                String regex = patternConfig.get("pattern");
                String severity = patternConfig.getOrDefault("severity", "MEDIUM");

                try {
                    Pattern compiled = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
                    var matcher = compiled.matcher(msg.getContent());

                    while (matcher.find()) {
                        Map<String, Object> match = new HashMap<>();
                        match.put("turnIndex", i);
                        match.put("role", msg.getRole());
                        match.put("patternName", patternName);
                        match.put("severity", severity);
                        match.put("matchedText", matcher.group());
                        matches.add(match);
                    }
                } catch (Exception e) {
                    log.warn("Invalid pattern '{}': {}", patternName, e.getMessage());
                }
            }
        }

        if (matches.isEmpty()) {
            return new ScorerResult(1.0, "No custom patterns matched", true);
        }

        double score = Math.max(0.0, 1.0 - (matches.size() * 0.25));
        String summary = String.format("%d custom pattern match(es) detected", matches.size());

        return ScorerResult.withDetails(score, summary, matches);
    }
}
