package com.pulsegpt.platform;

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
@Table(name = "platform_accounts", indexes = {
        @Index(name = "idx_platform_accounts_user_id", columnList = "user_id"),
        @Index(name = "idx_platform_accounts_platform", columnList = "platform"),
        @Index(name = "idx_platform_accounts_status", columnList = "status")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uq_platform_account_user_platform_ext", columnNames = {"user_id", "platform", "external_account_id"})
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlatformAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private PlatformType platform;

    @Column(name = "external_account_id", nullable = false)
    private String externalAccountId;

    @Column(name = "account_name", nullable = false)
    private String accountName;

    @Column(name = "access_token_encrypted")
    private String accessTokenEncrypted;

    @Column(name = "refresh_token_encrypted")
    private String refreshTokenEncrypted;

    @Column(name = "token_expires_at")
    private Instant tokenExpiresAt;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private PlatformAccountStatus status = PlatformAccountStatus.CONNECTED;

    @CreationTimestamp
    @Column(name = "connected_at", nullable = false, updatable = false)
    private Instant connectedAt;

    @Column(name = "disconnected_at")
    private Instant disconnectedAt;

    @Column(name = "last_synced_at")
    private Instant lastSyncedAt;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata", columnDefinition = "jsonb")
    private Map<String, Object> metadata;
}
