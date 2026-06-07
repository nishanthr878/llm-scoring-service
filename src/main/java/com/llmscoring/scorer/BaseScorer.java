package com.llmscoring.scorer;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;

@Slf4j
public abstract class BaseScorer {

    protected ScorerResult parseResponse(String response, String scorerName) {
        try {
            // Strip markdown code fences if LLM wraps response in ```json ... ```
            String cleaned = response
                    .replaceAll("```json", "")
                    .replaceAll("```", "")
                    .trim();

            int scoreIdx = cleaned.indexOf("\"score\":");
            int commaIdx = cleaned.indexOf(",", scoreIdx);
            String scoreStr = cleaned.substring(scoreIdx + 8, commaIdx).trim();
            double score = Double.parseDouble(scoreStr);

            int reasoningIdx = cleaned.indexOf("\"reasoning\":");
            int startQuote = cleaned.indexOf("\"", reasoningIdx + 12);
            int endQuote = cleaned.indexOf("\"", startQuote + 1);
            String reasoning = cleaned.substring(startQuote + 1, endQuote);

            return new ScorerResult(score, reasoning, true);
        } catch (Exception e) {
            log.warn("{} failed to parse response: {}", scorerName, response);
            return ScorerResult.error("Parse failed: " + response);
        }
    }

    protected String callWithRetry(ChatClient chatClient, String prompt, String scorerName) {
        int maxRetries = 2;
        Exception lastException = null;

        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                return chatClient.prompt()
                        .user(prompt)
                        .call()
                        .content();
            } catch (Exception e) {
                lastException = e;
                log.warn("{} attempt {}/{} failed: {}", scorerName, attempt, maxRetries, e.getMessage());
                if (attempt < maxRetries) {
                    try { Thread.sleep(500L * attempt); } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }
        throw new RuntimeException("All retries failed for " + scorerName, lastException);
    }
}
