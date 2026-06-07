package com.llmscoring.repository;

import com.llmscoring.model.Scenario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ScenarioRepository extends JpaRepository<Scenario, Long> {

    Optional<Scenario> findByName(String name);

    List<Scenario> findByActiveTrue();

    boolean existsByName(String name);
}
