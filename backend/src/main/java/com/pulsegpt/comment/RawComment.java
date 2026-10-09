package com.pulsegpt.comment;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "raw_comments", indexes = {
        @Index(name = "idx_raw_comments_post_id", columnList = "post_id"),
        @Index(name = "idx_raw_comments_external_id", columnList = "external_comment_id"),
        @Index(name = "idx_raw_comments_published_at", columnList = "published_at")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uq_raw_comments_post_external_id", columnNames = {"post_id", "external_comment_id"})
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RawComment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    @Column(name = "external_comment_id", nullable = false)
    private String externalCommentId;

    @Column(name = "author_external_id")
    private String authorExternalId;

    @Column(name = "author_display_name")
    private String authorDisplayName;

    @Column(name = "raw_text", nullable = false, columnDefinition = "TEXT")
    private String rawText;

    @Column(name = "published_at", nullable = false)
    private Instant publishedAt;

    @Builder.Default
    @Column(nullable = false)
    private int likes = 0;

    @Builder.Default
    @Column(nullable = false)
    private int replies = 0;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata", columnDefinition = "jsonb")
    private Map<String, Object> metadata;

    @CreationTimestamp
    @Column(name = "imported_at", nullable = false, updatable = false)
    private Instant importedAt;
}
