package com.pulsegpt.rag.dto;

import com.pulsegpt.recommendation.EvidenceSourceType;
import lombok.Builder;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Builder
public record RagEvidenceItem(
        String citationId,     // e.g. "[E1]", "[E2]"
        String evidenceId,     // e.g. "comment:uuid", "topic:uuid", "graph:topic-101"
        EvidenceSourceType sourceType, // COMMENT, TOPIC, QUESTION, TREND, MEMORY, CONTENT_HISTORY
        String sourceId,
        UUID userId,
        String text,           // Full text or synthesized summary
        double similarity,     // Vector cosine similarity [0.0, 1.0]
        double recency,        // Recency decay score [0.0, 1.0]
        double relevance,      // Relevance score [0.0, 1.0]
        double quality,        // Quality/Confidence score [0.0, 1.0]
        double evidenceScore,  // Canonical fused score: 0.40*relevance + 0.25*recency + 0.20*volume + 0.15*quality
        Map<String, Object> metadata,
        Instant createdAt
) {
    public RagEvidenceItem withCitationId(String newCitationId) {
        return new RagEvidenceItem(
                newCitationId,
                this.evidenceId,
                this.sourceType,
                this.sourceId,
                this.userId,
                this.text,
                this.similarity,
                this.recency,
                this.relevance,
                this.quality,
                this.evidenceScore,
                this.metadata,
                this.createdAt
        );
    }
}
