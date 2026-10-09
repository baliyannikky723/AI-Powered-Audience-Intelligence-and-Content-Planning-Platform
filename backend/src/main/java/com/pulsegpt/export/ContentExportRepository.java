package com.pulsegpt.export;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ContentExportRepository extends JpaRepository<ContentExport, UUID> {

    Optional<ContentExport> findByIdAndUserId(UUID id, UUID userId);

    Page<ContentExport> findAllByUserIdAndProductionAssetId(UUID userId, UUID productionAssetId, Pageable pageable);

    List<ContentExport> findAllByUserIdAndProductionAssetIdOrderByCreatedAtDesc(UUID userId, UUID productionAssetId);

    Page<ContentExport> findAllByUserId(UUID userId, Pageable pageable);

    long countByUserId(UUID userId);
}
