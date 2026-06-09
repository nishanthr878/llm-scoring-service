package com.llmscoring.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.llmscoring.model.Alert;
import com.llmscoring.model.ScoringResult;
import com.llmscoring.model.TurnFlag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

@Slf4j
@Service
@RequiredArgsConstructor
public class SseEmitterService {

    private final ObjectMapper objectMapper;
    private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();

    public SseEmitter createEmitter() {
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);
        emitters.add(emitter);

        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(() -> emitters.remove(emitter));
        emitter.onError(e -> emitters.remove(emitter));

        // Send initial event so browser confirms connection
        try {
            emitter.send(SseEmitter.event()
                    .name("Connected")
                    .data("ready"));
        } catch (Exception e) {
            emitters.remove(emitter);
            log.error("Error sending initial SSE event", e);
        }

        log.info("SSE client connected — total: {}", emitters.size());
        return emitter;
    }

    public void pushScoringResult(ScoringResult result, List<TurnFlag> flags) {
        push("scoring-result", Map.of(
                "result", result,
                "flags", flags,
                "flagCount", flags.size()
        ));
    }

    public void pushAlert(Alert alert) {
        push("alert", alert);
    }

    public void pushTurnFlag(TurnFlag flag) {
        push("turn-flag", flag);
    }

    private void push(String eventType, Object data) {
        List<SseEmitter> deadEmitters = new CopyOnWriteArrayList<>();

        for (SseEmitter emitter : emitters) {
            try {
                String json = objectMapper.writeValueAsString(data);
                emitter.send(SseEmitter.event()
                        .name(eventType)
                        .data(json));
            } catch (Exception e) {
                deadEmitters.add(emitter);
            }
        }

        emitters.removeAll(deadEmitters);
    }

    @Scheduled(fixedDelay = 15000)
    public void hearbeat() {
        push("heartbeat", Map.of("ts", System.currentTimeMillis()));
    }
}
