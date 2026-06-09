package com.llmscoring.service;

import com.llmscoring.model.Conversation;
import com.llmscoring.model.ScoringResult;
import com.llmscoring.model.TurnFlag;
import com.llmscoring.repository.ConversationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConversationService {

    private final ConversationRepository conversationRepository;

    public Conversation store(
            ScoringResult result,
            List<Map<String, Object>> messages,
            String scenarioName,
            List<TurnFlag> flags) {

        Conversation conv = new Conversation();
        conv.setSessionId(result.getSessionId());
        conv.setScenarioName(scenarioName);
        conv.setModelName(result.getModelName());
        conv.setPromptVersion(result.getPromptVersion());
        conv.setTurnCount(result.getTurnCount());
        conv.setScoringResultId(result.getId());
        conv.setOverallPassed(result.getOverallPassed());
        conv.setFlagCount(flags != null ? flags.size() : 0);

        // Convert messages to simple map format
        if (messages != null) {
            List<Map<String, String>> simplified = messages.stream()
                    .map(m -> Map.of(
                            "role", String.valueOf(m.getOrDefault("role", "")),
                            "content", String.valueOf(m.getOrDefault("content", ""))
                    ))
                    .collect(Collectors.toList());
            conv.setMessages(simplified);
        }

        Conversation saved = conversationRepository.save(conv);
        log.info("Stored conversation — session={} turns={} flags={}",
                saved.getSessionId(), saved.getTurnCount(), saved.getFlagCount());
        return saved;
    }

    public List<Conversation> getAll() {
        return conversationRepository.findAllByOrderByCreatedAtDesc();
    }

    public List<Conversation> getFailed() {
        return conversationRepository.findByOverallPassedFalseOrderByCreatedAtDesc();
    }

    public List<Conversation> getFlagged() {
        return conversationRepository.findByFlagCountGreaterThanOrderByCreatedAtDesc(0);
    }

    public List<Conversation> getByScenario(String scenarioName) {
        return conversationRepository.findByScenarioNameOrderByCreatedAtDesc(scenarioName);
    }

    public Conversation getById(Long id) {
        return conversationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Conversation not found: " + id));
    }
}
