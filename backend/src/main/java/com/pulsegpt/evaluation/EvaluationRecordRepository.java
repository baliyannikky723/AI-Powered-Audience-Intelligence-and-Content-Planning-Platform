package com.pulsegpt.evaluation;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface EvaluationRecordRepository extends JpaRepository<EvaluationRecord, UUID> {

    List<EvaluationRecord> findByUserIdAndTargetType(UUID userId, String targetType);

    Page<EvaluationRecord> findByUserIdAndTargetType(UUID userId, String targetType, Pageable pageable);

    List<EvaluationRecord> findByExperimentRunId(UUID experimentRunId);
}
