package com.llmscoring.repository;

import com.llmscoring.enums.ScoringType;
import com.llmscoring.model.ScoringResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface ScoringResultRepository extends JpaRepository<ScoringResult, Long> {

    List<ScoringResult> findBySessionId(String sessionId);

    List<ScoringResult> findByOverallPassedFalse();

    List<ScoringResult> findByModelName(String modelName);

    List<ScoringResult> findByType(ScoringType type);

    List<ScoringResult> findByScoredAtBetween(Instant from, Instant to);
}
