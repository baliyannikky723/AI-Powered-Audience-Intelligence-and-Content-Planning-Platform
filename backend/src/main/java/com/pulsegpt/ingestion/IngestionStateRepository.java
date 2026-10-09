package com.pulsegpt.ingestion;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface IngestionStateRepository extends JpaRepository<IngestionState, UUID> {

    Optional<IngestionState> findByPlatformAccountId(UUID platformAccountId);

    void deleteByPlatformAccountId(UUID platformAccountId);
}
