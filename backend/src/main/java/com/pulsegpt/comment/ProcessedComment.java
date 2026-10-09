package com.pulsegpt.comment;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "processed_comments", indexes = {
        @Index(name = "idx_processed_comments_raw_comment", columnList = "raw_comment_id"),
        @Index(name = "idx_processed_comments_language", columnList = "language"),
        @Index(name = "idx_processed_comments_sentiment", columnList = "sentiment_label"),
        @Index(name = "idx_processed_comments_intent", columnList = "intent"),
        @Index(name = "idx_processed_comments_processed_at", columnList = "processed_at"),
        @Index(name = "idx_processed_comments_priority", columnList = "priority")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProcessedComment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "raw_comment_id", nullable = false, unique = true)
    private RawComment rawComment;

    @Column(name = "normalized_text", columnDefinition = "TEXT")
    private String normalizedText;

    @Column(length = 16)
    private String language;

    @Builder.Default
    @Column(name = "is_hinglish")
    private Boolean isHinglish = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "sentiment_label", length = 32)
    private SentimentLabel sentimentLabel;

    @Column(name = "sentiment_score")
    private Double sentimentScore;

    @Enumerated(EnumType.STRING)
    @Column(length = 32)
    private IntentType intent;

    @Builder.Default
    @Column(name = "spam_score")
    private Double spamScore = 0.0;

    @Builder.Default
    @Column(name = "is_spam")
    private Boolean isSpam = false;

    @Builder.Default
    @Column(name = "is_duplicate")
    private Boolean isDuplicate = false;

    @Builder.Default
    @Column(name = "pii_masked")
    private Boolean piiMasked = false;

    @Enumerated(EnumType.STRING)
    @Column(length = 32)
    private Priority priority;

    @Column(name = "embedding", columnDefinition = "TEXT")
    private String embedding;

    @Column(name = "embedding_model", length = 64)
    private String embeddingModel;

    @Column(name = "embedding_dimension")
    private Integer embeddingDimension;

    @CreationTimestamp
    @Column(name = "processed_at", nullable = false, updatable = false)
    private Instant processedAt;

    @Column(name = "processing_version", length = 32)
    private String processingVersion;
}
