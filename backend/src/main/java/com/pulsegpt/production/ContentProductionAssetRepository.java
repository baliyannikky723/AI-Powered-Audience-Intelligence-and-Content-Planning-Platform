package com.pulsegpt.production;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ContentProductionAssetRepository extends JpaRepository<ContentProductionAsset, UUID> {

    Optional<ContentProductionAsset> findByIdAndUserId(UUID id, UUID userId);

    Page<ContentProductionAsset> findAllByUserId(UUID userId, Pageable pageable);

    Page<ContentProductionAsset> findAllByUserIdAndRecommendationId(UUID userId, UUID recommendationId, Pageable pageable);

    List<ContentProductionAsset> findAllByUserIdAndRecommendationId(UUID userId, UUID recommendationId);

    Page<ContentProductionAsset> findAllByUserIdAndCalendarItemId(UUID userId, UUID calendarItemId, Pageable pageable);

    Page<ContentProductionAsset> findAllByUserIdAndAssetType(UUID userId, ProductionAssetType assetType, Pageable pageable);

    Page<ContentProductionAsset> findAllByUserIdAndStatus(UUID userId, ProductionAssetStatus status, Pageable pageable);

    @Query("SELECT a FROM ContentProductionAsset a WHERE a.user.id = :userId " +
           "AND (:recommendationId IS NULL OR a.recommendation.id = :recommendationId) " +
           "AND (:calendarItemId IS NULL OR a.calendarItem.id = :calendarItemId) " +
           "AND (:assetType IS NULL OR a.assetType = :assetType) " +
           "AND (:status IS NULL OR a.status = :status)")
    Page<ContentProductionAsset> searchAssets(
            @Param("userId") UUID userId,
            @Param("recommendationId") UUID recommendationId,
            @Param("calendarItemId") UUID calendarItemId,
            @Param("assetType") ProductionAssetType assetType,
            @Param("status") ProductionAssetStatus status,
            Pageable pageable
    );

    long countByUserIdAndStatus(UUID userId, ProductionAssetStatus status);

    long countByUserId(UUID userId);
}
