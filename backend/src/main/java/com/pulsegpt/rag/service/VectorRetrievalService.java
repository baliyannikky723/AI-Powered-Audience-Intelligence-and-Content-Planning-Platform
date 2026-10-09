package com.pulsegpt.rag.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pulsegpt.ai.client.AiServiceClient;
import com.pulsegpt.ai.client.dto.AiEmbedRequest;
import com.pulsegpt.ai.client.dto.AiEmbedResponse;
import com.pulsegpt.comment.ProcessedComment;
import com.pulsegpt.comment.ProcessedCommentRepository;
import com.pulsegpt.platform.PlatformType;
import com.pulsegpt.rag.dto.RagEvidenceItem;
import com.pulsegpt.recommendation.EvidenceSourceType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class VectorRetrievalService {

    private final ProcessedCommentRepository processedCommentRepository;
    private final AiServiceClient aiServiceClient;
    private final ObjectMapper objectMapper;

    private static final Pattern EMAIL_PATTERN = Pattern.compile("[\\w\\.-]+@[\\w\\.-]+\\.\\w+");
    private static final Pattern PHONE_PATTERN = Pattern.compile("\\b\\d{3}[-.\\s]?\\d{3}[-.\\s]?\\d{4}\\b");

    @Transactional(readOnly = true)
    public List<RagEvidenceItem> retrieveSemanticComments(
            UUID userId,
            String queryText,
            PlatformType platform,
            int timeRangeDays,
            int topK,
            double minSimilarityThreshold) {

        long startTime = System.currentTimeMillis();
        Instant cutoff = Instant.now().minus(Duration.ofDays(timeRangeDays));

        // 1. Fetch eligible processed comments with embeddings
        List<ProcessedComment> candidates;
        if (platform != null) {
            candidates = processedCommentRepository.findEligibleForClusteringByUserIdPlatformAndDateRange(
                    userId, platform, cutoff, Instant.now(), PageRequest.of(0, 100));
        } else {
            candidates = processedCommentRepository.findEligibleForClusteringByUserIdAndDateRange(
                    userId, cutoff, Instant.now(), PageRequest.of(0, 100));
        }

        if (candidates.isEmpty()) {
            candidates = processedCommentRepository.findEligibleForClusteringByUserId(userId, PageRequest.of(0, 100));
        }

        if (candidates.isEmpty()) {
            log.debug("No eligible candidate comments found for vector retrieval user {}", userId);
            return Collections.emptyList();
        }

        // 2. Generate or extract query embedding vector
        List<Double> queryVec = generateQueryEmbedding(queryText);

        // 3. Score candidate comments with Cosine Similarity
        List<ScoredComment> scoredList = new ArrayList<>();
        for (ProcessedComment pc : candidates) {
            if (pc.getEmbedding() == null || pc.getEmbedding().isBlank()) continue;

            List<Double> docVec = parseEmbeddingVector(pc.getEmbedding());
            if (docVec.isEmpty()) continue;

            double similarity = computeCosineSimilarity(queryVec, docVec);
            if (similarity >= minSimilarityThreshold) {
                scoredList.add(new ScoredComment(pc, similarity));
            }
        }

        // 4. Sort descending by similarity, tie-break deterministically by ID
        scoredList.sort((a, b) -> {
            int cmp = Double.compare(b.similarity(), a.similarity());
            if (cmp != 0) return cmp;
            return a.comment().getId().compareTo(b.comment().getId());
        });

        // 5. Transform topK into RagEvidenceItems
        List<RagEvidenceItem> results = new ArrayList<>();
        int limit = Math.min(topK, scoredList.size());
        for (int i = 0; i < limit; i++) {
            ScoredComment sc = scoredList.get(i);
            ProcessedComment pc = sc.comment();
            String rawText = pc.getNormalizedText() != null ? pc.getNormalizedText() : pc.getRawComment().getRawText();
            String cleanText = sanitizePii(rawText);

            Instant publishedAt = pc.getRawComment().getPublishedAt() != null
                    ? pc.getRawComment().getPublishedAt()
                    : pc.getProcessedAt();

            long daysOld = Duration.between(publishedAt != null ? publishedAt : Instant.now(), Instant.now()).toDays();
            double recency = Math.max(0.1, 1.0 - (daysOld / 180.0));
            double quality = pc.getSentimentScore() != null ? Math.abs(pc.getSentimentScore()) : 0.8;
            double relevance = sc.similarity();

            // Canonical Evidence Score: 0.40*relevance + 0.25*recency + 0.20*volume + 0.15*quality
            double volume = 0.5; // Single comment observation base volume
            double evidenceScore = Math.round(((0.40 * relevance) + (0.25 * recency) + (0.20 * volume) + (0.15 * quality)) * 100.0) / 100.0;

            Map<String, Object> meta = new HashMap<>();
            meta.put("intent", pc.getIntent() != null ? pc.getIntent().name() : "FEEDBACK");
            meta.put("sentiment", pc.getSentimentLabel() != null ? pc.getSentimentLabel().name() : "NEUTRAL");
            meta.put("similarity", Math.round(sc.similarity() * 1000.0) / 1000.0);
            if (pc.getRawComment().getAuthorDisplayName() != null) {
                meta.put("author", "[REDACTED_AUTHOR]");
            }

            results.add(RagEvidenceItem.builder()
                    .evidenceId("comment:" + pc.getId())
                    .sourceType(EvidenceSourceType.COMMENT)
                    .sourceId(pc.getId().toString())
                    .userId(userId)
                    .text(cleanText)
                    .similarity(sc.similarity())
                    .recency(recency)
                    .relevance(relevance)
                    .quality(quality)
                    .evidenceScore(evidenceScore)
                    .metadata(meta)
                    .createdAt(publishedAt)
                    .build());
        }

        log.debug("Vector retrieval found {} semantic comments in {}ms for query: '{}'",
                results.size(), System.currentTimeMillis() - startTime, queryText);
        return results;
    }

    private List<Double> generateQueryEmbedding(String queryText) {
        try {
            AiEmbedResponse resp = aiServiceClient.generateEmbeddings(
                    new AiEmbedRequest(List.of(queryText))
            );
            if (resp != null && resp.embeddings() != null && !resp.embeddings().isEmpty()) {
                return resp.embeddings().get(0);
            }
        } catch (Exception e) {
            log.warn("AI service embedding generation failed, falling back to deterministic sparse vector: {}", e.getMessage());
        }
        return generateDeterministicVector(queryText, 384);
    }

    private List<Double> parseEmbeddingVector(String raw) {
        if (raw == null || raw.isBlank()) return Collections.emptyList();
        try {
            return objectMapper.readValue(raw, new TypeReference<List<Double>>() {});
        } catch (Exception ex) {
            try {
                String clean = raw.replace("[", "").replace("]", "").trim();
                if (clean.isEmpty()) return Collections.emptyList();
                String[] tokens = clean.split(",");
                List<Double> vec = new ArrayList<>(tokens.length);
                for (String t : tokens) {
                    vec.add(Double.parseDouble(t.trim()));
                }
                return vec;
            } catch (Exception e) {
                return Collections.emptyList();
            }
        }
    }

    public double computeCosineSimilarity(List<Double> vecA, List<Double> vecB) {
        if (vecA == null || vecB == null || vecA.isEmpty() || vecB.isEmpty()) return 0.0;
        int size = Math.min(vecA.size(), vecB.size());
        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;
        for (int i = 0; i < size; i++) {
            double a = vecA.get(i);
            double b = vecB.get(i);
            dotProduct += a * b;
            normA += a * a;
            normB += b * b;
        }
        if (normA <= 0.0 || normB <= 0.0) return 0.0;
        double sim = dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
        return Math.max(0.0, Math.min(1.0, sim));
    }

    private List<Double> generateDeterministicVector(String text, int dim) {
        List<Double> vec = new ArrayList<>(dim);
        String[] words = text.toLowerCase().split("\\W+");
        for (int i = 0; i < dim; i++) {
            double val = 0.0;
            for (String w : words) {
                if (!w.isBlank()) {
                    int hash = (w.hashCode() ^ (i * 31));
                    val += Math.sin(hash);
                }
            }
            vec.add(val);
        }
        // Normalize
        double norm = 0.0;
        for (double v : vec) norm += v * v;
        norm = Math.sqrt(norm);
        if (norm > 0) {
            for (int i = 0; i < dim; i++) {
                vec.set(i, vec.get(i) / norm);
            }
        }
        return vec;
    }

    private String sanitizePii(String text) {
        if (text == null) return "";
        String clean = EMAIL_PATTERN.matcher(text).replaceAll("[EMAIL_REDACTED]");
        clean = PHONE_PATTERN.matcher(clean).replaceAll("[PHONE_REDACTED]");
        return clean.trim();
    }

    private record ScoredComment(ProcessedComment comment, double similarity) {}
}
