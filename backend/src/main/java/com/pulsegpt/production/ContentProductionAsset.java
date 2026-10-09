package com.pulsegpt.production;

import com.pulsegpt.calendar.CalendarItem;
import com.pulsegpt.recommendation.ContentRecommendation;
import com.pulsegpt.recommendation.GenerationMode;
import com.pulsegpt.user.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "content_production_assets", indexes = {
        @Index(name = "idx_prod_assets_user_id", columnList = "user_id"),
        @Index(name = "idx_prod_assets_recommendation_id", columnList = "recommendation_id"),
        @Index(name = "idx_prod_assets_calendar_item_id", columnList = "calendar_item_id"),
        @Index(name = "idx_prod_assets_asset_type", columnList = "asset_type"),
        @Index(name = "idx_prod_assets_status", columnList = "status"),
        @Index(name = "idx_prod_assets_created_at", columnList = "created_at"),
        @Index(name = "idx_prod_assets_user_rec", columnList = "user_id, recommendation_id")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContentProductionAsset {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recommendation_id", nullable = false)
    private ContentRecommendation recommendation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "calendar_item_id")
    private CalendarItem calendarItem;

    @Enumerated(EnumType.STRING)
    @Column(name = "asset_type", nullable = false, length = 64)
    private ProductionAssetType assetType;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private ProductionAssetStatus status = ProductionAssetStatus.GENERATED;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "generation_mode", nullable = false, length = 32)
    private GenerationMode generationMode = GenerationMode.EVIDENCE_GROUNDED;

    @Builder.Default
    @Column(name = "version", nullable = false)
    private Integer version = 1;

    @Column(name = "prompt_version", length = 32)
    private String promptVersion;

    @Column(name = "model_name", length = 64)
    private String modelName;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "content_json", columnDefinition = "jsonb", nullable = false)
    private Map<String, Object> contentJson;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "evidence_snapshot", columnDefinition = "jsonb")
    private Map<String, Object> evidenceSnapshot;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "validation_json", columnDefinition = "jsonb")
    private Map<String, Object> validationJson;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "revision_history", columnDefinition = "jsonb")
    private List<Map<String, Object>> revisionHistory;

    @Builder.Default
    @Column(name = "repair_attempted", nullable = false)
    private Boolean repairAttempted = false;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "repair_result_json", columnDefinition = "jsonb")
    private Map<String, Object> repairResultJson;

    @Column(name = "approved_at")
    private Instant approvedAt;

    @Column(name = "approved_by")
    private UUID approvedBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
