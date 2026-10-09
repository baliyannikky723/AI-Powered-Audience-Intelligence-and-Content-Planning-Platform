package com.pulsegpt.topic.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pulsegpt.ai.client.AiServiceClient;
import com.pulsegpt.ai.client.dto.*;
import com.pulsegpt.audit.AuditService;
import com.pulsegpt.comment.ProcessedComment;
import com.pulsegpt.comment.ProcessedCommentRepository;
import com.pulsegpt.common.exception.ApiException;
import com.pulsegpt.memory.service.AudienceMemoryService;
import com.pulsegpt.topic.*;
import com.pulsegpt.topic.dto.ClusteringSummaryResponse;
import com.pulsegpt.topic.dto.ClusteringTriggerRequest;
import com.pulsegpt.topic.dto.TopicResponse;
import com.pulsegpt.topic.mapper.TopicMapper;
import com.pulsegpt.user.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AudienceClusteringService {

    private final ProcessedCommentRepository processedCommentRepository;
    private final TopicRepository topicRepository;
    private final TopicAssignmentRepository topicAssignmentRepository;
    private final ClusteringRunRepository clusteringRunRepository;
    private final AiServiceClient aiServiceClient;
    private final AuditService auditService;
    private final TopicMapper topicMapper;
    private final ObjectMapper objectMapper;
    private final AudienceMemoryService audienceMemoryService;

    private static final double CENTROID_SIMILARITY_THRESHOLD = 0.82;

    @Transactional
    public ClusteringSummaryResponse executeClustering(User currentUser, ClusteringTriggerRequest request) {
        int maxComments = request.getMaxCommentsOrDefault();
        log.info("CLUSTERING_STARTED: user={} maxComments={} platform={}", currentUser.getId(), maxComments, request.platform());

        auditService.logAuditEvent(currentUser, "CLUSTERING_STARTED", currentUser.getId().toString(),
                Map.of("maxComments", maxComments, "platform", request.platform() != null ? request.platform().name() : "ALL"));

        // 1. Fetch eligible processed comments
        List<ProcessedComment> eligibleComments = fetchEligibleComments(currentUser.getId(), request, maxComments);

        // 2. Initialize ClusteringRun entity
        ClusteringRun run = ClusteringRun.builder()
                .user(currentUser)
                .status(ClusteringRunStatus.IN_PROGRESS)
                .timeWindowStart(request.startDate())
                .timeWindowEnd(request.endDate())
                .inputCommentCount(eligibleComments.size())
                .algorithm(request.algorithm() != null ? request.algorithm() : "SEMANTIC_UMAP_HDBSCAN_CTFIDF")
                .algorithmVersion("pulsegpt-cluster-v1")
                .embeddingModel("sentence-transformers/all-MiniLM-L6-v2")
                .embeddingDimension(384)
                .config(Map.of(
                        "algorithm", request.algorithm() != null ? request.algorithm() : "SEMANTIC_UMAP_HDBSCAN_CTFIDF",
                        "minClusterSize", request.minClusterSize() != null ? request.minClusterSize() : 3,
                        "umapEnabled", request.umapEnabled() != null ? request.umapEnabled() : true,
                        "kClusters", request.kClusters() != null ? request.kClusters() : 0
                ))
                .build();
        run = clusteringRunRepository.save(run);

        if (eligibleComments.isEmpty()) {
            log.info("CLUSTERING_COMPLETED: No eligible comments found for user {}", currentUser.getId());
            run.setStatus(ClusteringRunStatus.COMPLETED);
            run.setCompletedAt(Instant.now());
            clusteringRunRepository.save(run);

            return ClusteringSummaryResponse.builder()
                    .runId(run.getId())
                    .status("COMPLETED")
                    .totalSubmitted(0)
                    .clusteredCount(0)
                    .noiseCount(0)
                    .clusterCount(0)
                    .matchedExistingTopicsCount(0)
                    .newTopicsCreatedCount(0)
                    .topics(Collections.emptyList())
                    .metrics(Collections.emptyMap())
                    .modelVersions(Collections.emptyMap())
                    .completedAt(Instant.now())
                    .build();
        }

        // 3. Prepare AI Microservice Payload
        Map<String, ProcessedComment> commentMap = new HashMap<>();
        List<AiClusteringCommentInput> aiCommentInputs = new ArrayList<>();

        for (ProcessedComment pc : eligibleComments) {
            List<Double> embeddingVec = parseEmbedding(pc.getEmbedding());
            if (embeddingVec.isEmpty()) {
                continue;
            }
            commentMap.put(pc.getId().toString(), pc);

            String pubDateStr = (pc.getRawComment().getPublishedAt() != null)
                    ? pc.getRawComment().getPublishedAt().toString() : null;
            String platformStr = (pc.getRawComment().getPost().getPlatformAccount() != null)
                    ? pc.getRawComment().getPost().getPlatformAccount().getPlatform().name() : null;

            aiCommentInputs.add(AiClusteringCommentInput.builder()
                    .commentId(pc.getId().toString())
                    .text(pc.getNormalizedText() != null ? pc.getNormalizedText() : pc.getRawComment().getRawText())
                    .embedding(embeddingVec)
                    .language(pc.getLanguage())
                    .sentiment(pc.getSentimentLabel() != null ? pc.getSentimentLabel().name() : null)
                    .intent(pc.getIntent() != null ? pc.getIntent().name() : null)
                    .platform(platformStr)
                    .publishedAt(pubDateStr)
                    .build());
        }

        AiClusteringConfig aiConfig = AiClusteringConfig.builder()
                .algorithm(request.algorithm() != null ? request.algorithm() : "SEMANTIC_UMAP_HDBSCAN_CTFIDF")
                .umapEnabled(request.umapEnabled() != null ? request.umapEnabled() : true)
                .randomState(42)
                .minClusterSize(request.minClusterSize() != null ? request.minClusterSize() : 3)
                .kClusters(request.kClusters())
                .topKeywords(10)
                .build();

        AiClusteringRunRequest aiRequest = AiClusteringRunRequest.builder()
                .runId(run.getId().toString())
                .comments(aiCommentInputs)
                .config(aiConfig)
                .build();

        // 4. Call AI Microservice
        AiClusteringRunResponse aiResponse;
        try {
            aiResponse = aiServiceClient.clusterComments(aiRequest);
        } catch (Exception ex) {
            log.error("CLUSTERING_FAILED for run {}: {}", run.getId(), ex.getMessage());
            run.setStatus(ClusteringRunStatus.FAILED);
            run.setErrorMessage(ex.getMessage());
            run.setCompletedAt(Instant.now());
            clusteringRunRepository.save(run);
            throw new ApiException("Clustering execution failed: " + ex.getMessage(), HttpStatus.SERVICE_UNAVAILABLE, "CLUSTERING_FAILED");
        }

        // 5. Stable Topic Identity Matching & Topic Persistence
        List<Topic> existingTopics = topicRepository.findByUserIdAndActiveTrue(currentUser.getId());
        Map<Integer, Topic> clusterIdToTopicMap = new HashMap<>();
        List<Topic> updatedTopics = new ArrayList<>();
        int matchedCount = 0;
        int newCreatedCount = 0;

        if (aiResponse.topics() != null) {
            for (AiClusterTopicResult clusterTopic : aiResponse.topics()) {
                List<Double> clusterCentroid = clusterTopic.centroid();
                Topic matchedTopic = findBestMatchingTopic(existingTopics, clusterCentroid, CENTROID_SIMILARITY_THRESHOLD);

                Map<String, Double> keywordScoresMap = new HashMap<>();
                if (clusterTopic.keywords() != null) {
                    for (AiClusterKeywordScore kw : clusterTopic.keywords()) {
                        keywordScoresMap.put(kw.keyword(), kw.score());
                    }
                }

                if (matchedTopic != null) {
                    // Stable Topic Identity Matched!
                    log.info("TOPIC_MATCHED: Cluster {} matched existing Topic {} ('{}')",
                            clusterTopic.clusterId(), matchedTopic.getId(), matchedTopic.getName());
                    matchedTopic.setCommentCount(matchedTopic.getCommentCount() + clusterTopic.commentCount());
                    matchedTopic.setKeywords(clusterTopic.rawKeywords());
                    matchedTopic.setKeywordScores(keywordScoresMap);
                    matchedTopic.setSentimentDistribution(mergeDistributions(matchedTopic.getSentimentDistribution(), clusterTopic.sentimentDistribution()));
                    matchedTopic.setIntentDistribution(mergeDistributions(matchedTopic.getIntentDistribution(), clusterTopic.intentDistribution()));
                    matchedTopic.setLanguageDistribution(mergeDistributions(matchedTopic.getLanguageDistribution(), clusterTopic.languageDistribution()));
                    matchedTopic.setPlatformDistribution(mergeDistributions(matchedTopic.getPlatformDistribution(), clusterTopic.platformDistribution()));
                    matchedTopic.setLastSeenAt(Instant.now());
                    matchedTopic.setLastClusteringRun(run);

                    // Update centroid via exponential moving average
                    List<Double> oldCentroid = parseEmbedding(matchedTopic.getCentroid());
                    List<Double> updatedCentroid = blendCentroids(oldCentroid, clusterCentroid, 0.7);
                    matchedTopic.setCentroid(serializeCentroid(updatedCentroid));

                    matchedTopic = topicRepository.save(matchedTopic);
                    clusterIdToTopicMap.put(clusterTopic.clusterId(), matchedTopic);
                    updatedTopics.add(matchedTopic);
                    matchedCount++;
                } else {
                    // Create New Topic Identity
                    log.info("TOPIC_CREATED: Created new Topic for cluster {} ('{}')",
                            clusterTopic.clusterId(), clusterTopic.label());
                    Topic newTopic = Topic.builder()
                            .user(currentUser)
                            .name(clusterTopic.label())
                            .description("Discovered audience topic representing recurring feedback and inquiries.")
                            .keywords(clusterTopic.rawKeywords())
                            .keywordScores(keywordScoresMap)
                            .commentCount(clusterTopic.commentCount())
                            .centroid(serializeCentroid(clusterCentroid))
                            .algorithm(run.getAlgorithm())
                            .algorithmVersion(run.getAlgorithmVersion())
                            .sentimentDistribution(clusterTopic.sentimentDistribution())
                            .intentDistribution(clusterTopic.intentDistribution())
                            .languageDistribution(clusterTopic.languageDistribution())
                            .platformDistribution(clusterTopic.platformDistribution())
                            .firstSeenAt(Instant.now())
                            .lastSeenAt(Instant.now())
                            .lastClusteringRun(run)
                            .active(true)
                            .build();

                    newTopic = topicRepository.save(newTopic);
                    existingTopics.add(newTopic);
                    clusterIdToTopicMap.put(clusterTopic.clusterId(), newTopic);
                    updatedTopics.add(newTopic);
                    newCreatedCount++;
                }
            }
        }

        // 6. Persist Topic Assignments
        if (aiResponse.assignments() != null) {
            List<TopicAssignment> assignmentsToSave = new ArrayList<>();
            for (AiCommentAssignmentResult assignmentResult : aiResponse.assignments()) {
                ProcessedComment pc = commentMap.get(assignmentResult.commentId());
                if (pc == null) {
                    continue;
                }

                Topic targetTopic = assignmentResult.isNoise() ? null : clusterIdToTopicMap.get(assignmentResult.clusterId());

                TopicAssignment assignment = TopicAssignment.builder()
                        .topic(targetTopic)
                        .processedComment(pc)
                        .clusteringRun(run)
                        .clusterId(assignmentResult.clusterId())
                        .isNoise(Boolean.TRUE.equals(assignmentResult.isNoise()))
                        .membershipProbability(assignmentResult.membershipProbability())
                        .build();

                assignmentsToSave.add(assignment);
            }
            topicAssignmentRepository.saveAll(assignmentsToSave);
            log.info("TOPIC_ASSIGNMENT_COMPLETED: Saved {} assignments for run {}", assignmentsToSave.size(), run.getId());
        }

        // 7. Update ClusteringRun Metadata
        Map<String, Object> metricsMap = new HashMap<>();
        if (aiResponse.metrics() != null) {
            metricsMap.put("clusterCount", aiResponse.metrics().clusterCount());
            metricsMap.put("noiseCount", aiResponse.metrics().noiseCount());
            metricsMap.put("noiseRatio", aiResponse.metrics().noiseRatio());
            metricsMap.put("largestClusterSize", aiResponse.metrics().largestClusterSize());
            metricsMap.put("smallestClusterSize", aiResponse.metrics().smallestClusterSize());
            metricsMap.put("averageClusterSize", aiResponse.metrics().averageClusterSize());
            metricsMap.put("silhouetteScore", aiResponse.metrics().silhouetteScore());
            metricsMap.put("daviesBouldinIndex", aiResponse.metrics().daviesBouldinIndex());
            metricsMap.put("calinskiHarabaszScore", aiResponse.metrics().calinskiHarabaszScore());
            metricsMap.put("metricWarnings", aiResponse.metrics().metricWarnings());
        }

        run.setStatus(ClusteringRunStatus.COMPLETED);
        run.setCompletedAt(Instant.now());
        run.setClusteredCommentCount(aiResponse.clusteredCount() != null ? aiResponse.clusteredCount() : 0);
        run.setNoiseCount(aiResponse.noiseCount() != null ? aiResponse.noiseCount() : 0);
        run.setClusterCount(aiResponse.clusterCount() != null ? aiResponse.clusterCount() : 0);
        run.setMetrics(metricsMap);
        clusteringRunRepository.save(run);

        auditService.logAuditEvent(currentUser, "CLUSTERING_COMPLETED", run.getId().toString(),
                Map.of("clusterCount", run.getClusterCount(), "noiseCount", run.getNoiseCount(),
                        "matchedTopics", matchedCount, "newTopics", newCreatedCount));

        // 8. Update Audience Memory & Project Knowledge Graph
        try {
            audienceMemoryService.aggregateAndUpdateUserMemory(currentUser.getId());
        } catch (Exception memEx) {
            log.warn("Post-clustering memory update failed for user {}: {}", currentUser.getId(), memEx.getMessage());
        }

        List<TopicResponse> topicResponses = topicMapper.toResponseList(updatedTopics);

        return ClusteringSummaryResponse.builder()
                .runId(run.getId())
                .status("COMPLETED")
                .totalSubmitted(aiCommentInputs.size())
                .clusteredCount(run.getClusteredCommentCount())
                .noiseCount(run.getNoiseCount())
                .clusterCount(run.getClusterCount())
                .matchedExistingTopicsCount(matchedCount)
                .newTopicsCreatedCount(newCreatedCount)
                .topics(topicResponses)
                .metrics(metricsMap)
                .modelVersions(aiResponse.modelVersions())
                .completedAt(run.getCompletedAt())
                .build();
    }

    private List<ProcessedComment> fetchEligibleComments(UUID userId, ClusteringTriggerRequest request, int limit) {
        PageRequest pageRequest = PageRequest.of(0, limit);
        if (request.startDate() != null && request.endDate() != null) {
            if (request.platform() != null) {
                return processedCommentRepository.findEligibleForClusteringByUserIdPlatformAndDateRange(
                        userId, request.platform(), request.startDate(), request.endDate(), pageRequest);
            } else {
                return processedCommentRepository.findEligibleForClusteringByUserIdAndDateRange(
                        userId, request.startDate(), request.endDate(), pageRequest);
            }
        } else if (request.platform() != null) {
            return processedCommentRepository.findEligibleForClusteringByUserIdAndPlatform(
                    userId, request.platform(), pageRequest);
        } else {
            return processedCommentRepository.findEligibleForClusteringByUserId(userId, pageRequest);
        }
    }

    private Topic findBestMatchingTopic(List<Topic> topics, List<Double> clusterCentroid, double threshold) {
        if (topics == null || topics.isEmpty() || clusterCentroid == null || clusterCentroid.isEmpty()) {
            return null;
        }

        Topic bestMatch = null;
        double highestSimilarity = -1.0;

        for (Topic t : topics) {
            List<Double> topicCentroid = parseEmbedding(t.getCentroid());
            if (topicCentroid.isEmpty() || topicCentroid.size() != clusterCentroid.size()) {
                continue;
            }

            double sim = computeCosineSimilarity(topicCentroid, clusterCentroid);
            if (sim > highestSimilarity && sim >= threshold) {
                highestSimilarity = sim;
                bestMatch = t;
            }
        }

        return bestMatch;
    }

    private double computeCosineSimilarity(List<Double> vecA, List<Double> vecB) {
        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;

        for (int i = 0; i < vecA.size(); i++) {
            double a = vecA.get(i);
            double b = vecB.get(i);
            dotProduct += a * b;
            normA += a * a;
            normB += b * b;
        }

        if (normA <= 0.0 || normB <= 0.0) {
            return 0.0;
        }
        return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    private List<Double> blendCentroids(List<Double> oldCentroid, List<Double> newCentroid, double alpha) {
        if (oldCentroid.isEmpty()) return newCentroid;
        if (newCentroid.isEmpty()) return oldCentroid;

        List<Double> blended = new ArrayList<>(oldCentroid.size());
        double norm = 0.0;
        for (int i = 0; i < oldCentroid.size(); i++) {
            double v = alpha * oldCentroid.get(i) + (1.0 - alpha) * newCentroid.get(i);
            blended.add(v);
            norm += v * v;
        }

        norm = Math.sqrt(norm);
        if (norm > 0) {
            for (int i = 0; i < blended.size(); i++) {
                blended.set(i, blended.get(i) / norm);
            }
        }
        return blended;
    }

    private List<Double> parseEmbedding(String rawEmbedding) {
        if (rawEmbedding == null || rawEmbedding.isBlank()) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(rawEmbedding, new TypeReference<List<Double>>() {});
        } catch (Exception ex) {
            // Fallback comma-delimited parse
            try {
                String clean = rawEmbedding.replace("[", "").replace("]", "").trim();
                if (clean.isEmpty()) return Collections.emptyList();
                String[] parts = clean.split(",");
                List<Double> result = new ArrayList<>(parts.length);
                for (String p : parts) {
                    result.add(Double.parseDouble(p.trim()));
                }
                return result;
            } catch (Exception e) {
                log.warn("Failed to parse embedding vector: {}", rawEmbedding);
                return Collections.emptyList();
            }
        }
    }

    private String serializeCentroid(List<Double> centroid) {
        try {
            return objectMapper.writeValueAsString(centroid);
        } catch (Exception e) {
            return centroid.toString();
        }
    }

    private Map<String, Integer> mergeDistributions(Map<String, Integer> existing, Map<String, Integer> incoming) {
        Map<String, Integer> merged = new HashMap<>();
        if (existing != null) merged.putAll(existing);
        if (incoming != null) {
            incoming.forEach((k, v) -> merged.put(k, merged.getOrDefault(k, 0) + (v != null ? v : 0)));
        }
        return merged;
    }
}
