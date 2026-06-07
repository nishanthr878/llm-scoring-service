package com.llmscoring.service;

import com.llmscoring.dto.ConversationRequest;
import com.llmscoring.dto.TraceRequest;
import com.llmscoring.engine.FlagEngine;
import com.llmscoring.enums.ScoringType;
import com.llmscoring.model.ScoringResult;
import com.llmscoring.repository.ScoringResultRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ScoringService {

    private final FlagEngine flagEngine;
    private final ScoringResultRepository repository;

    public ScoringResult evaluate(TraceRequest request) {
        log.info("Evaluating single turn — session={} model={}",
                request.getSessionId(), request.getModelName());
        ScoringResult result = flagEngine.evaluate(
                request.toContext(),
                ScoringType.SINGLE_TURN
        );
        return repository.save(result);
    }

    public ScoringResult evaluateConversation(ConversationRequest request) {
        log.info("Evaluating conversation — session={} turns={}",
                request.getSessionId(), request.getMessages().size());
        ScoringResult result = flagEngine.evaluate(
                request.toContext(),
                ScoringType.CONVERSATION
        );
        return repository.save(result);
    }

    public List<ScoringResult> getAll() {
        return repository.findAll();
    }

    public List<ScoringResult> getFailed() {
        return repository.findByOverallPassedFalse();
    }

    public List<ScoringResult> getBySession(String sessionId) {
        return repository.findBySessionId(sessionId);
    }

    public ScoringResult getById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RuntimeException("ScoringResult not found: " + id));
    }
}
