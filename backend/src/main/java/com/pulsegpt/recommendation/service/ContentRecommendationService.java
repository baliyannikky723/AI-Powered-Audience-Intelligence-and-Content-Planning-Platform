package com.pulsegpt.recommendation.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pulsegpt.ai.client.AiServiceClient;
import com.pulsegpt.ai.client.dto.*;
import com.pulsegpt.audit.AuditService;
import com.pulsegpt.comment.Priority;
import com.pulsegpt.common.dto.PageResponse;
import com.pulsegpt.common.exception.ApiException;
import com.pulsegpt.common.exception.ResourceNotFoundException;
import com.pulsegpt.common.util.PaginationUtils;
import com.pulsegpt.recommendation.*;
import com.pulsegpt.recommendation.dto.*;
import com.pulsegpt.recommendation.mapper.ContentRecommendationMapper;
import com.pulsegpt.security.RateLimitingService;
import com.pulsegpt.topic.Topic;
import com.pulsegpt.topic.TopicRepository;
import com.pulsegpt.user.User;
import com.pulsegpt.validation.CheckResult;
import com.pulsegpt.validation.RecommendationValidator;
import com.pulsegpt.validation.ValidationResult;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ContentRecommendationService {

    private final ContentRecommendationRepository recommendationRepository;
    private final TopicRepository topicRepository;
    private final EvidenceRetrievalService evidenceRetrievalService;
    private final RecommendationValidator recommendationValidator;
    private final AiServiceClient aiServiceClient;
    private final RateLimitingService rateLimitingService;
    private final AuditService auditService;
    private final ContentRecommendationMapper recommendationMapper;
    private final ObjectMapper objectMapper;

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "createdAt", "updatedAt", "confidence", "status", "priority", "title"
    );

    @Transactional
    public List<RecommendationResponse> generateRecommendations(User user, RecommendationGenerateRequest request) {
        // 1. Enforce AI Rate Limiting
        if (!rateLimitingService.tryConsumeAi(user.getId())) {
            throw new ApiException("AI recommendation rate limit exceeded", HttpStatus.TOO_MANY_REQUESTS, "RATE_LIMIT_EXCEEDED");
        }

        String requestId = UUID.randomUUID().toString();
        log.info("RECOMMENDATION_GENERATION_STARTED: user={} requestId={} topicId={} mode={}",
                user.getId(), requestId, request.topicId(), request.getModeOrDefault());

        auditService.logAuditEvent(user, "RECOMMENDATION_GENERATION_STARTED", requestId,
                Map.of("topicId", request.topicId() != null ? request.topicId().toString() : "NONE",
                        "mode", request.getModeOrDefault().name()));

        // 2. Evidence Retrieval & Context Construction
        RecommendationContext context = evidenceRetrievalService.retrieveEvidence(user, request);

        Topic targetTopic = null;
        if (request.topicId() != null) {
            targetTopic = topicRepository.findByIdAndUserId(request.topicId(), user.getId()).orElse(null);
        } else if (!context.topics().isEmpty()) {
            targetTopic = context.topics().get(0);
        }

        // 3. Prepare AI Microservice Payload
        List<AiEvidenceItem> aiEvidenceList = context.evidenceItems().stream()
                .map(e -> AiEvidenceItem.builder()
                        .evidenceId(e.evidenceId())
                        .sourceType(e.sourceType().name())
                        .sourceId(e.sourceId().toString())
                        .summary(e.summary())
                        .relevanceScore(e.relevanceScore())
                        .metadata(e.metadata())
                        .build())
                .toList();

        List<Map<String, Object>> contentHistoryPayload = context.contentHistory().stream()
                .map(p -> Map.<String, Object>of("id", p.getId().toString(), "title", p.getTitle()))
                .toList();

        List<Map<String, Object>> previousRecsPayload = context.previousRecommendations().stream()
                .map(r -> Map.<String, Object>of("id", r.getId().toString(), "title", r.getTitle()))
                .toList();

        AiRecommendationGenerateRequest aiRequest = AiRecommendationGenerateRequest.builder()
                .requestId(requestId)
                .topicId(targetTopic != null ? targetTopic.getId().toString() : null)
                .topicName(targetTopic != null ? targetTopic.getName() : null)
                .contentType(request.getContentTypeOrDefault())
                .goal(request.getGoalOrDefault())
                .targetAudience(request.targetAudience() != null ? request.targetAudience() : "Tech Enthusiasts & Creators")
                .maxIdeas(request.getMaxIdeasOrDefault())
                .mode(request.getModeOrDefault().name())
                .evidence(aiEvidenceList)
                .contentHistory(contentHistoryPayload)
                .previousRecommendations(previousRecsPayload)
                .build();

        // 4. Call AI Microservice
        AiRecommendationGenerateResponse aiResponse;
        try {
            aiResponse = aiServiceClient.generateRecommendations(aiRequest);
        } catch (Exception ex) {
            log.error("AI recommendation call failed: {}", ex.getMessage());
            throw new ApiException("Failed to generate content recommendations: " + ex.getMessage(),
                    HttpStatus.SERVICE_UNAVAILABLE, "AI_RECOMMENDATION_FAILED");
        }

        log.info("RECOMMENDATION_GENERATED: Received {} drafts from AI service",
                aiResponse.drafts() != null ? aiResponse.drafts().size() : 0);

        // 5. Six-Check Validation & One-Shot Repair Loop
        List<ContentRecommendation> savedRecommendations = new ArrayList<>();

        if (aiResponse.drafts() != null) {
            for (AiRecommendationDraft draft : aiResponse.drafts()) {
                ValidationResult validation = recommendationValidator.validate(draft, context, request.getModeOrDefault());
                boolean repairAttempted = false;
                Map<String, Object> repairResultMap = null;
                AiRecommendationDraft finalDraft = draft;

                if (!validation.valid()) {
                    log.info("VALIDATION_COMPLETED: Initial validation failed ({}) for draft '{}'. Initiating one-shot repair.",
                            validation.failureSummary(), draft.title());

                    log.info("RECOMMENDATION_REPAIR_STARTED: Requesting AI repair for draft '{}'", draft.title());
                    repairAttempted = true;

                    List<Map<String, Object>> failedChecksList = validation.checks().stream()
                            .filter(c -> !c.passed())
                            .map(c -> Map.<String, Object>of("name", c.checkName(), "reason", c.reason()))
                            .toList();

                    AiRecommendationRepairRequest repairReq = AiRecommendationRepairRequest.builder()
                            .requestId(requestId + "-repair")
                            .originalDraft(draft)
                            .failedChecks(failedChecksList)
                            .evidence(aiEvidenceList)
                            .mode(request.getModeOrDefault().name())
                            .build();

                    try {
                        AiRecommendationRepairResponse repairRes = aiServiceClient.repairRecommendation(repairReq);
                        finalDraft = repairRes.repairedDraft();
                        // Re-validate repaired draft
                        validation = recommendationValidator.validate(finalDraft, context, request.getModeOrDefault());

                        repairResultMap = Map.of(
                                "explanation", repairRes.repairExplanation() != null ? repairRes.repairExplanation() : "",
                                "originalTitle", draft.title(),
                                "repairedTitle", finalDraft.title(),
                                "revalidationPassed", validation.valid()
                        );
                        log.info("RECOMMENDATION_REPAIR_COMPLETED: Repair result valid={}", validation.valid());
                    } catch (Exception rex) {
                        log.warn("Repair attempt failed: {}", rex.getMessage());
                        repairResultMap = Map.of("error", rex.getMessage(), "revalidationPassed", false);
                    }
                } else {
                    log.info("VALIDATION_COMPLETED: Draft passed all 6 checks successfully");
                }

                RecommendationStatus status = validation.valid() ? RecommendationStatus.VALIDATED : RecommendationStatus.REJECTED;

                if (status == RecommendationStatus.VALIDATED) {
                    log.info("RECOMMENDATION_ACCEPTED: Saved validated recommendation '{}'", finalDraft.title());
                } else {
                    log.warn("RECOMMENDATION_REJECTED: Draft failed validation after repair attempt: {}", validation.failureSummary());
                }

                // 6. Build & Persist ContentRecommendation Entity
                Map<String, Object> evidenceSnapshotMap = buildEvidenceSnapshot(finalDraft, context);
                Map<String, Object> draftJsonMap = convertObjectToMap(finalDraft);
                Map<String, Object> validationJsonMap = convertObjectToMap(validation);

                ContentRecommendation entity = ContentRecommendation.builder()
                        .user(user)
                        .topic(targetTopic)
                        .title(finalDraft.title())
                        .description(finalDraft.angle() + "\n\n" + finalDraft.problemAddressed())
                        .contentType(finalDraft.contentType())
                        .angle(finalDraft.angle())
                        .targetAudience(finalDraft.targetAudience())
                        .problemAddress(finalDraft.problemAddressed())
                        .hook(finalDraft.hook())
                        .callToAction(finalDraft.callToAction())
                        .keyPoints(finalDraft.keyPoints())
                        .priority(Priority.HIGH)
                        .reason(finalDraft.reason())
                        .status(status)
                        .generationMode(request.getModeOrDefault())
                        .confidence(finalDraft.confidence())
                        .validationPassed(validation.valid())
                        .evidenceSnapshot(evidenceSnapshotMap)
                        .draftJson(draftJsonMap)
                        .validationJson(validationJsonMap)
                        .repairAttempted(repairAttempted)
                        .repairResultJson(repairResultMap)
                        .llmModel(aiResponse.modelName())
                        .promptVersion(aiResponse.promptVersion())
                        .build();

                entity = recommendationRepository.save(entity);
                savedRecommendations.add(entity);

                auditService.logAuditEvent(user, status == RecommendationStatus.VALIDATED ? "RECOMMENDATION_ACCEPTED" : "RECOMMENDATION_REJECTED",
                        entity.getId().toString(),
                        Map.of("title", entity.getTitle(), "status", status.name(), "confidence", entity.getConfidence()));
            }
        }

        return recommendationMapper.toResponseList(savedRecommendations);
    }

    @Transactional(readOnly = true)
    public PageResponse<RecommendationResponse> getRecommendations(User currentUser, RecommendationQuery query) {
        Pageable pageable = PaginationUtils.createPageRequest(
                query.getPageOrDefault(),
                query.getSizeOrDefault(),
                query.sort(),
                query.direction(),
                "createdAt",
                ALLOWED_SORT_FIELDS
        );

        Specification<ContentRecommendation> spec = buildRecommendationSpecification(query, currentUser.getId());
        Page<ContentRecommendation> page = recommendationRepository.findAll(spec, pageable);
        List<RecommendationResponse> responses = recommendationMapper.toResponseList(page.getContent());

        return PageResponse.from(page, responses);
    }

    @Transactional
    public RecommendationResponse approveRecommendation(User currentUser, UUID id) {
        ContentRecommendation rec = recommendationRepository.findByIdAndUserId(id, currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Recommendation not found with id: " + id));

        // Idempotent if already approved
        if (rec.getStatus() == RecommendationStatus.APPROVED) {
            log.info("Recommendation {} already approved by user {}", id, currentUser.getId());
            return recommendationMapper.toResponse(rec);
        }

        // Only VALIDATED recommendations can be approved
        if (rec.getStatus() != RecommendationStatus.VALIDATED) {
            throw new ApiException(
                    "Only VALIDATED recommendations can be approved. Current status: " + rec.getStatus(),
                    HttpStatus.BAD_REQUEST,
                    "INVALID_RECOMMENDATION_STATUS"
            );
        }

        rec.setStatus(RecommendationStatus.APPROVED);
        rec.setApprovedAt(Instant.now());
        rec.setApprovedBy(currentUser.getId());
        rec = recommendationRepository.save(rec);

        auditService.logAuditEvent(currentUser, "RECOMMENDATION_APPROVED", rec.getId().toString(),
                Map.of(
                        "recommendationId", rec.getId().toString(),
                        "topicId", rec.getTopic() != null ? rec.getTopic().getId().toString() : "NONE",
                        "title", rec.getTitle(),
                        "approvedAt", rec.getApprovedAt().toString()
                ));

        log.info("RECOMMENDATION_APPROVED: user={} recId={}", currentUser.getId(), rec.getId());
        return recommendationMapper.toResponse(rec);
    }

    @Transactional(readOnly = true)
    public RecommendationResponse getRecommendationById(User currentUser, UUID id) {
        ContentRecommendation rec = recommendationRepository.findByIdAndUserId(id, currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Recommendation not found with id: " + id));
        return recommendationMapper.toResponse(rec);
    }

    private Specification<ContentRecommendation> buildRecommendationSpecification(RecommendationQuery query, UUID userId) {
        return (root, criteriaQuery, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // 1. Mandatory User Scoping
            predicates.add(cb.equal(root.get("user").get("id"), userId));

            // 2. Topic Filter
            if (query.topicId() != null) {
                predicates.add(cb.equal(root.get("topic").get("id"), query.topicId()));
            }

            // 3. Status Filter
            if (query.status() != null) {
                predicates.add(cb.equal(root.get("status"), query.status()));
            }

            // 4. Generation Mode Filter
            if (query.mode() != null) {
                predicates.add(cb.equal(root.get("generationMode"), query.mode()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private Map<String, Object> buildEvidenceSnapshot(AiRecommendationDraft draft, RecommendationContext context) {
        Map<String, Object> snapshot = new HashMap<>();
        List<Map<String, Object>> items = new ArrayList<>();

        Set<String> referencedIds = new HashSet<>(draft.evidenceIds() != null ? draft.evidenceIds() : Collections.emptyList());

        for (EvidenceItem ev : context.evidenceItems()) {
            if (referencedIds.contains(ev.evidenceId())) {
                items.add(Map.of(
                        "evidenceId", ev.evidenceId(),
                        "sourceType", ev.sourceType().name(),
                        "sourceId", ev.sourceId().toString(),
                        "summary", ev.summary(),
                        "relevanceScore", ev.relevanceScore()
                ));
            }
        }

        snapshot.put("evidenceCount", items.size());
        snapshot.put("items", items);
        snapshot.put("timestamp", Instant.now().toString());
        return snapshot;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> convertObjectToMap(Object obj) {
        if (obj == null) return Collections.emptyMap();
        try {
            return objectMapper.convertValue(obj, new TypeReference<Map<String, Object>>() {});
        } catch (Exception ex) {
            return Collections.emptyMap();
        }
    }
}
