package com.pulsegpt.evaluation;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ExperimentRunRepository extends JpaRepository<ExperimentRun, UUID> {

    List<ExperimentRun> findByExperimentType(String experimentType);

    Page<ExperimentRun> findByUserId(UUID userId, Pageable pageable);

    Optional<ExperimentRun> findByIdAndUserId(UUID id, UUID userId);

    List<ExperimentRun> findByStatus(String status);
}
