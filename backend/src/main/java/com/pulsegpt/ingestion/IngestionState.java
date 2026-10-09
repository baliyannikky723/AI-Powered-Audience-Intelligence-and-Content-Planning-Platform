package com.pulsegpt.ingestion;

import com.pulsegpt.platform.PlatformAccount;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "ingestion_state", indexes = {
        @Index(name = "idx_ingestion_state_platform_account", columnList = "platform_account_id"),
        @Index(name = "idx_ingestion_state_next_sync", columnList = "next_sync_at")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IngestionState {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "platform_account_id", nullable = false, unique = true)
    private PlatformAccount platformAccount;

    @Column(name = "cursor_token", columnDefinition = "TEXT")
    private String cursorToken;

    @Column(name = "last_successful_sync_at")
    private Instant lastSuccessfulSyncAt;

    @Column(name = "next_sync_at")
    private Instant nextSyncAt;

    @Builder.Default
    @Column(name = "failure_count", nullable = false)
    private int failureCount = 0;

    @Column(name = "backoff_until")
    private Instant backoffUntil;
}
