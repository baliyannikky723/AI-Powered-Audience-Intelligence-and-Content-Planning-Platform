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
@Table(name = "evaluation_records", indexes = {
        @Index(name = "idx_evaluation_records_user_created", columnList = "user_id, created_at"),
        @Index(name = "idx_evaluation_records_user_type", columnList = "user_id, target_type"),
        @Index(name = "idx_evaluation_records_run_id", columnList = "experiment_run_id")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EvaluationRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "experiment_run_id")
    private ExperimentRun experimentRun;

    @Column(name = "target_type", nullable = false, length = 64)
    private String targetType;

    @Column(name = "target_id", nullable = false)
    private UUID targetId;

    @Column(name = "generation_mode", nullable = false, length = 64)
    private String generationMode;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metrics_json", columnDefinition = "jsonb", nullable = false)
    private Map<String, Object> metrics;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
