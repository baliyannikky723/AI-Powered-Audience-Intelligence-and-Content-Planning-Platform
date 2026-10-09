package com.pulsegpt.export;

import com.pulsegpt.production.ContentProductionAsset;
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
@Table(name = "content_exports", indexes = {
        @Index(name = "idx_content_exports_user_id", columnList = "user_id"),
        @Index(name = "idx_content_exports_user_created", columnList = "user_id, created_at"),
        @Index(name = "idx_content_exports_user_asset", columnList = "user_id, production_asset_id"),
        @Index(name = "idx_content_exports_user_type", columnList = "user_id, export_type")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContentExport {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "production_asset_id", nullable = false)
    private ContentProductionAsset productionAsset;

    @Enumerated(EnumType.STRING)
    @Column(name = "export_type", nullable = false, length = 64)
    private ExportType exportType;

    @Column(name = "file_name", nullable = false)
    private String fileName;

    @Column(name = "mime_type", nullable = false, length = 128)
    private String mimeType;

    @Column(name = "content_hash", nullable = false, length = 128)
    private String contentHash;

    @Builder.Default
    @Column(name = "version", nullable = false)
    private Integer version = 1;

    @Column(name = "file_size_bytes")
    private Long fileSizeBytes;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata_json", columnDefinition = "jsonb")
    private Map<String, Object> metadataJson;

    @Column(name = "content_data")
    private byte[] contentData;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "expires_at")
    private Instant expiresAt;
}
