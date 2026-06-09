package com.llmscoring.repository;

import com.llmscoring.model.Alert;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AlertRepository extends JpaRepository<Alert, Long> {
    List<Alert> findByDismissedFalseOrderByFiredAtDesc();
    List<Alert> findBySessionId(String sessionId);
    List<Alert> findByScenarioNameAndDismissedFalse(String scenarioName);
    long countByDismissedFalse();
}
