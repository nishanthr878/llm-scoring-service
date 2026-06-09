package com.llmscoring.repository;

import com.llmscoring.enums.FlagType;
import com.llmscoring.model.TurnFlag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TurnFlagRepository extends JpaRepository<TurnFlag, Long> {
    List<TurnFlag> findByScoringResultId(Long scoringResultId);
    List<TurnFlag> findBySessionId(String sessionId);
    List<TurnFlag> findBySessionIdAndFlagType(String sessionId, FlagType flagType);
    List<TurnFlag> findByDismissedFalse();
    List<TurnFlag> findByScenarioNameAndDismissedFalse(String scenarioName);
}
