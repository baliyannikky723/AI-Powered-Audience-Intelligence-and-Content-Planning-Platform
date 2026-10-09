package com.pulsegpt.admin;

import com.pulsegpt.platform.PlatformAccount;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "api_quota_usage", indexes = {
        @Index(name = "idx_api_quota_platform_account", columnList = "platform_account_id"),
        @Index(name = "idx_api_quota_usage_date", columnList = "usage_date")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uq_api_quota_account_date", columnNames = {"platform_account_id", "usage_date"})
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiQuotaUsage {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "platform_account_id", nullable = false)
    private PlatformAccount platformAccount;

    @Column(name = "usage_date", nullable = false)
    private LocalDate usageDate;

    @Builder.Default
    @Column(name = "quota_used", nullable = false)
    private long quotaUsed = 0L;

    @Builder.Default
    @Column(name = "quota_limit", nullable = false)
    private long quotaLimit = 10000L;

    @Builder.Default
    @Column(name = "warning_threshold", nullable = false)
    private int warningThreshold = 80;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
