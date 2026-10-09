package com.pulsegpt.ingestion;

import com.pulsegpt.platform.PlatformAccount;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "ingestion_runs", indexes = {
        @Index(name = "idx_ingestion_runs_platform_account", columnList = "platform_account_id"),
        @Index(name = "idx_ingestion_runs_status", columnList = "status"),
        @Index(name = "idx_ingestion_runs_started_at", columnList = "started_at")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IngestionRun {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "platform_account_id", nullable = false)
    private PlatformAccount platformAccount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private IngestionRunStatus status;

    @CreationTimestamp
    @Column(name = "started_at", nullable = false, updatable = false)
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Builder.Default
    @Column(name = "fetched_count", nullable = false)
    private int fetchedCount = 0;

    @Builder.Default
    @Column(name = "inserted_count", nullable = false)
    private int insertedCount = 0;

    @Builder.Default
    @Column(name = "duplicate_count", nullable = false)
    private int duplicateCount = 0;

    @Builder.Default
    @Column(name = "failed_count", nullable = false)
    private int failedCount = 0;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;
}
