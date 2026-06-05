package com.llmscoring.repository;

import com.llmscoring.model.ScoringResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface ScoringResultRepository extends JpaRepository<ScoringResult, Long> {

    List<ScoringResult> findBySessionId(String sessionId);

    List<ScoringResult> findByOverallPassedFalse();

    List<ScoringResult> findByModelName(String modelName);

    @Query("SELECT s FROM ScoringResult s WHERE s.scoredAt BETWEEN :from AND :to")
    List<ScoringResult> findBetween(Instant from, Instant to);

    @Query("SELECT AVG(s.faithfulnessScore) FROM ScoringResult s WHERE s.modelName = :modelName")
    Double avgFaithfulnessByModel(String modelName);

    @Query("SELECT AVG(s.answerRelevanceScore) FROM ScoringResult s WHERE s.modelName = :modelName")
    Double avgRelevanceByModel(String modelName);
}
