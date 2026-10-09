package com.pulsegpt.ingestion;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface IngestionRunRepository extends JpaRepository<IngestionRun, UUID> {

    List<IngestionRun> findByPlatformAccountIdOrderByStartedAtDesc(UUID platformAccountId);

    Page<IngestionRun> findByPlatformAccountId(UUID platformAccountId, Pageable pageable);

    List<IngestionRun> findByStatus(IngestionRunStatus status);

    boolean existsByPlatformAccountIdAndStatus(UUID platformAccountId, IngestionRunStatus status);
}
