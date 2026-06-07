package com.llmscoring.repository;

import com.llmscoring.enums.ScoringType;
import com.llmscoring.model.ScoringResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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

    long countByOverallPassedTrue();
    long countByOverallPassedFalse();
    long countByOverallPassedIsNull();
    long countByType(ScoringType type);

    // Distinct model names
    @Query("SELECT DISTINCT s.modelName FROM ScoringResult s WHERE s.modelName IS NOT NULL")
    List<String> findDistinctModelNames();

    // Results by model
    List<ScoringResult> findByModelNameAndScoredAtBetween(
            String modelName, Instant from, Instant to);

    // Daily trend — native query for date truncation
    @Query(value = """
            SELECT
                DATE(scored_at) as date,
                COUNT(*) as total,
                SUM(CASE WHEN overall_passed = true THEN 1 ELSE 0 END) as passed,
                SUM(CASE WHEN overall_passed = false THEN 1 ELSE 0 END) as failed
            FROM scoring_results
            WHERE scored_at >= :from
            GROUP BY DATE(scored_at)
            ORDER BY DATE(scored_at) ASC
            """, nativeQuery = true)
    List<Object[]> findDailyTrend(@Param("from") Instant from);

    // Scenario name from JSONB context — native query
    @Query(value = """
            SELECT
                scores::text,
                overall_passed,
                model_name
            FROM scoring_results
            WHERE type = 'SCENARIO'
            """, nativeQuery = true)
    List<Object[]> findScenarioRawData();
}
