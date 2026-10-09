package com.pulsegpt.topic;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ClusteringRunRepository extends JpaRepository<ClusteringRun, UUID>, JpaSpecificationExecutor<ClusteringRun> {

    Optional<ClusteringRun> findByIdAndUserId(UUID id, UUID userId);

    Page<ClusteringRun> findByUserId(UUID userId, Pageable pageable);

    long countByUserId(UUID userId);
}
