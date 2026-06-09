package com.llmscoring.repository;

import com.llmscoring.model.Conversation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ConversationRepository extends JpaRepository<Conversation, Long> {
    List<Conversation> findAllByOrderByCreatedAtDesc();
    List<Conversation> findByOverallPassedFalseOrderByCreatedAtDesc();
    List<Conversation> findByScenarioNameOrderByCreatedAtDesc(String scenarioName);
    Optional<Conversation> findBySessionIdAndScoringResultId(String sessionId, Long scoringResultId);
    List<Conversation> findBySessionIdOrderByCreatedAtDesc(String sessionId);
    List<Conversation> findByFlagCountGreaterThanOrderByCreatedAtDesc(int count);
}
