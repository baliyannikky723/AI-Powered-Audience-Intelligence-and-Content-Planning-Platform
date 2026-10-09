package com.pulsegpt.topic;

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
@Table(name = "clustering_runs", indexes = {
        @Index(name = "idx_clustering_runs_user_id", columnList = "user_id"),
        @Index(name = "idx_clustering_runs_status", columnList = "status"),
        @Index(name = "idx_clustering_runs_started_at", columnList = "started_at")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClusteringRun {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @CreationTimestamp
    @Column(name = "started_at", nullable = false, updatable = false)
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private ClusteringRunStatus status;

    @Column(name = "time_window_start")
    private Instant timeWindowStart;

    @Column(name = "time_window_end")
    private Instant timeWindowEnd;

    @Builder.Default
    @Column(name = "input_comment_count", nullable = false)
    private int inputCommentCount = 0;

    @Builder.Default
    @Column(name = "clustered_comment_count", nullable = false)
    private int clusteredCommentCount = 0;

    @Builder.Default
    @Column(name = "noise_count", nullable = false)
    private int noiseCount = 0;

    @Builder.Default
    @Column(name = "cluster_count", nullable = false)
    private int clusterCount = 0;

    @Column(nullable = false, length = 64)
    private String algorithm;

    @Column(name = "algorithm_version", length = 32)
    private String algorithmVersion;

    @Column(name = "embedding_model", length = 64)
    private String embeddingModel;

    @Column(name = "embedding_dimension")
    private Integer embeddingDimension;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "config", columnDefinition = "jsonb")
    private Map<String, Object> config;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metrics", columnDefinition = "jsonb")
    private Map<String, Object> metrics;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
