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
@Table(name = "experiment_runs", indexes = {
        @Index(name = "idx_experiment_runs_user_id", columnList = "user_id"),
        @Index(name = "idx_experiment_runs_user_created", columnList = "user_id, created_at"),
        @Index(name = "idx_experiment_runs_type", columnList = "experiment_type"),
        @Index(name = "idx_experiment_runs_status", columnList = "status"),
        @Index(name = "idx_experiment_runs_started_at", columnList = "started_at")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExperimentRun {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "experiment_name", nullable = false)
    private String experimentName;

    @Column(name = "experiment_type", nullable = false, length = 64)
    private String experimentType;

    @Column(name = "description", columnDefinition = "text")
    private String description;

    @Builder.Default
    @Column(name = "baseline_mode", length = 64)
    private String baselineMode = "BASELINE";

    @Builder.Default
    @Column(name = "treatment_mode", length = 64)
    private String treatmentMode = "EVIDENCE_GROUNDED";

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "configuration", columnDefinition = "jsonb")
    private Map<String, Object> configuration;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metrics", columnDefinition = "jsonb")
    private Map<String, Object> metrics;

    @Column(name = "dataset_snapshot_id")
    private UUID datasetSnapshotId;

    @Column(name = "model_version", length = 64)
    private String modelVersion;

    @Column(name = "prompt_version", length = 64)
    private String promptVersion;

    @Column(name = "algorithm_version", length = 32)
    private String algorithmVersion;

    @Builder.Default
    @Column(name = "seed")
    private Integer seed = 42;

    @CreationTimestamp
    @Column(name = "started_at", nullable = false, updatable = false)
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Builder.Default
    @Column(nullable = false, length = 32)
    private String status = "CREATED";

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
