package com.pulsegpt.evaluation;

import com.pulsegpt.user.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "dataset_snapshots", indexes = {
        @Index(name = "idx_dataset_snapshots_user_created", columnList = "user_id, created_at"),
        @Index(name = "idx_dataset_snapshots_platform", columnList = "user_id, platform")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DatasetSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "text")
    private String description;

    @Column(length = 64)
    private String platform;

    @Column(name = "date_from")
    private Instant dateFrom;

    @Column(name = "date_to")
    private Instant dateTo;

    @Builder.Default
    @Column(name = "comment_count", nullable = false)
    private Integer commentCount = 0;

    @Builder.Default
    @Column(name = "processed_comment_count", nullable = false)
    private Integer processedCommentCount = 0;

    @Column(name = "embedding_model", length = 128)
    private String embeddingModel;

    @Column(name = "processing_version", length = 64)
    private String processingVersion;

    @Column(name = "clustering_version", length = 64)
    private String clusteringVersion;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "snapshot_metadata_json", columnDefinition = "jsonb")
    private Map<String, Object> snapshotMetadata;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
