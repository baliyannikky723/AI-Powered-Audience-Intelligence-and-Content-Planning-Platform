package com.pulsegpt.evaluation;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface DatasetSnapshotRepository extends JpaRepository<DatasetSnapshot, UUID> {

    Page<DatasetSnapshot> findByUserId(UUID userId, Pageable pageable);

    Optional<DatasetSnapshot> findByIdAndUserId(UUID id, UUID userId);
}
