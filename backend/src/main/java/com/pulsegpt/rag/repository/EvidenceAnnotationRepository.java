package com.pulsegpt.rag.repository;

import com.pulsegpt.rag.model.EvidenceAnnotation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface EvidenceAnnotationRepository extends JpaRepository<EvidenceAnnotation, UUID> {

    List<EvidenceAnnotation> findByUserIdOrderByCreatedAtDesc(UUID userId);

    Page<EvidenceAnnotation> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    List<EvidenceAnnotation> findByUserIdAndQueryId(UUID userId, String queryId);

    List<EvidenceAnnotation> findByUserIdAndEvidenceId(UUID userId, String evidenceId);

    long countByUserId(UUID userId);
}
