package com.pulsegpt.comment.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pulsegpt.ai.client.AiServiceClient;
import com.pulsegpt.ai.client.dto.*;
import com.pulsegpt.audit.AuditService;
import com.pulsegpt.comment.*;
import com.pulsegpt.comment.dto.CommentProcessingSummaryResponse;
import com.pulsegpt.comment.dto.CommentProcessingTriggerRequest;
import com.pulsegpt.config.AppProperties;
import com.pulsegpt.user.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CommentProcessingService {

    private final RawCommentRepository rawCommentRepository;
    private final ProcessedCommentRepository processedCommentRepository;
    private final AiServiceClient aiServiceClient;
    private final AuditService auditService;
    private final AppProperties appProperties;
    private final ObjectMapper objectMapper;

    @Transactional
    public CommentProcessingSummaryResponse processComments(User user, CommentProcessingTriggerRequest request) {
        int limit = Math.min(request.getLimit(), 500);
        int batchSize = Math.max(1, appProperties.getAiService().getMaxBatchSize());

        log.info("Starting comment processing for user {} with limit {}", user.getId(), limit);
        auditService.logAuditEvent(user, "COMMENT_PROCESSING_STARTED", user.getId().toString(),
                Map.of("limit", limit));

        List<RawComment> rawComments;
        if (request.getPlatform() != null) {
            rawComments = rawCommentRepository.findUnprocessedByUserIdAndPlatform(
                    user.getId(), request.getPlatform(), PageRequest.of(0, limit));
        } else {
            rawComments = rawCommentRepository.findUnprocessedByUserId(user.getId(), PageRequest.of(0, limit));
        }

        if (rawComments.isEmpty()) {
            log.info("No unprocessed comments found for user {}", user.getId());
            return CommentProcessingSummaryResponse.builder()
                    .status("COMPLETED")
                    .totalSubmitted(0)
                    .processedCount(0)
                    .failedCount(0)
                    .duplicateCount(0)
                    .spamCount(0)
                    .piiMaskedCount(0)
                    .modelVersions(Collections.emptyMap())
                    .completedAt(Instant.now())
                    .build();
        }

        int totalSubmitted = rawComments.size();
        int processedCount = 0;
        int failedCount = 0;
        int duplicateCount = 0;
        int spamCount = 0;
        int piiMaskedCount = 0;
        Map<String, String> lastModelVersions = new HashMap<>();

        // Group into batches
        for (int i = 0; i < rawComments.size(); i += batchSize) {
            List<RawComment> batch = rawComments.subList(i, Math.min(i + batchSize, rawComments.size()));
            Map<String, RawComment> rawCommentMap = batch.stream()
                    .collect(Collectors.toMap(r -> r.getId().toString(), r -> r));

            List<AiCommentProcessRequest> aiRequests = batch.stream()
                    .map(rc -> new AiCommentProcessRequest(
                            rc.getId().toString(),
                            rc.getRawText(),
                            null,
                            null
                    ))
                    .toList();

            try {
                AiBatchCommentProcessResponse batchResponse = aiServiceClient.processBatch(
                        new AiBatchCommentProcessRequest(aiRequests));

                if (batchResponse != null && batchResponse.results() != null) {
                    for (AiCommentProcessResponse itemResult : batchResponse.results()) {
                        RawComment rawComment = rawCommentMap.get(itemResult.commentId());
                        if (rawComment == null) {
                            continue;
                        }

                        if (Boolean.FALSE.equals(itemResult.success())) {
                            log.warn("Per-item failure for comment {}: {}", itemResult.commentId(), itemResult.errorMessage());
                            failedCount++;
                            continue;
                        }

                        // Idempotency: Check if ProcessedComment already exists
                        Optional<ProcessedComment> existing = processedCommentRepository.findByRawCommentId(rawComment.getId());
                        ProcessedComment pc = existing.orElseGet(() -> ProcessedComment.builder()
                                .rawComment(rawComment)
                                .build());

                        pc.setNormalizedText(itemResult.normalizedText());
                        pc.setLanguage(itemResult.language());
                        pc.setIsHinglish(Boolean.TRUE.equals(itemResult.isHinglish()));

                        if (itemResult.sentiment() != null) {
                            pc.setSentimentLabel(itemResult.sentiment().label());
                            pc.setSentimentScore(itemResult.sentiment().score());
                        }

                        if (itemResult.intent() != null) {
                            pc.setIntent(itemResult.intent().label());
                        }

                        pc.setSpamScore(itemResult.spamScore() != null ? itemResult.spamScore() : 0.0);
                        pc.setIsSpam(Boolean.TRUE.equals(itemResult.isSpam()));
                        pc.setIsDuplicate(Boolean.TRUE.equals(itemResult.isDuplicate()));
                        pc.setPiiMasked(Boolean.TRUE.equals(itemResult.piiDetected()));
                        pc.setPriority(itemResult.priority() != null ? itemResult.priority() : Priority.MEDIUM);

                        // Embedding serialization
                        if (itemResult.embedding() != null && !itemResult.embedding().isEmpty()) {
                            try {
                                pc.setEmbedding(objectMapper.writeValueAsString(itemResult.embedding()));
                            } catch (Exception ex) {
                                pc.setEmbedding(itemResult.embedding().toString());
                            }
                            pc.setEmbeddingDimension(itemResult.embeddingDimension() != null ? itemResult.embeddingDimension() : 384);
                            if (itemResult.modelVersions() != null) {
                                pc.setEmbeddingModel(itemResult.modelVersions().embedding());
                            }
                        }

                        if (itemResult.modelVersions() != null) {
                            pc.setProcessingVersion(itemResult.modelVersions().algorithm());
                            lastModelVersions.put("language", itemResult.modelVersions().language());
                            lastModelVersions.put("sentiment", itemResult.modelVersions().sentiment());
                            lastModelVersions.put("intent", itemResult.modelVersions().intent());
                            lastModelVersions.put("embedding", itemResult.modelVersions().embedding());
                            lastModelVersions.put("algorithm", itemResult.modelVersions().algorithm());
                        }

                        processedCommentRepository.save(pc);
                        processedCount++;

                        if (Boolean.TRUE.equals(itemResult.isDuplicate())) {
                            duplicateCount++;
                        }
                        if (Boolean.TRUE.equals(itemResult.isSpam())) {
                            spamCount++;
                        }
                        if (Boolean.TRUE.equals(itemResult.piiDetected())) {
                            piiMaskedCount++;
                        }
                    }
                }
            } catch (Exception ex) {
                log.error("Batch processing failed for batch starting at {}: {}", i, ex.getMessage());
                failedCount += batch.size();
            }
        }

        String finalStatus = (failedCount == 0) ? "COMPLETED" : (processedCount > 0 ? "PARTIAL" : "FAILED");
        log.info("Comment processing completed with status {}. Processed: {}, Failed: {}", finalStatus, processedCount, failedCount);

        auditService.logAuditEvent(user, "COMMENT_PROCESSING_COMPLETED", user.getId().toString(),
                Map.of("processedCount", processedCount, "failedCount", failedCount, "status", finalStatus));

        return CommentProcessingSummaryResponse.builder()
                .status(finalStatus)
                .totalSubmitted(totalSubmitted)
                .processedCount(processedCount)
                .failedCount(failedCount)
                .duplicateCount(duplicateCount)
                .spamCount(spamCount)
                .piiMaskedCount(piiMaskedCount)
                .modelVersions(lastModelVersions)
                .completedAt(Instant.now())
                .build();
    }
}
