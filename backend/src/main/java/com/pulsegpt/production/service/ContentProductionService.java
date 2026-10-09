package com.pulsegpt.production.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pulsegpt.ai.client.AiServiceClient;
import com.pulsegpt.ai.client.dto.*;
import com.pulsegpt.audit.AuditService;
import com.pulsegpt.calendar.CalendarItem;
import com.pulsegpt.calendar.CalendarItemRepository;
import com.pulsegpt.common.exception.ApiException;
import com.pulsegpt.production.*;
import com.pulsegpt.production.dto.*;
import com.pulsegpt.production.validation.ProductionAssetValidator;
import com.pulsegpt.recommendation.*;
import com.pulsegpt.recommendation.dto.EvidenceItem;
import com.pulsegpt.recommendation.dto.RecommendationContext;
import com.pulsegpt.recommendation.dto.RecommendationGenerateRequest;
import com.pulsegpt.recommendation.service.EvidenceRetrievalService;
import com.pulsegpt.user.User;
import com.pulsegpt.validation.CheckResult;
import com.pulsegpt.validation.ValidationResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ContentProductionService {

    private final ContentProductionAssetRepository productionAssetRepository;
    private final ContentRecommendationRepository recommendationRepository;
    private final CalendarItemRepository calendarItemRepository;
    private final EvidenceRetrievalService evidenceRetrievalService;
    private final AiServiceClient aiServiceClient;
    private final ProductionAssetValidator productionAssetValidator;
    private final AuditService auditService;
    private final ObjectMapper objectMapper;

    @Transactional
    public List<ProductionAssetResponse> generateProductionAssets(User user, ProductionGenerateRequest request) {
        log.info("Starting production asset generation for user={}, recId={}, calId={}",
                user.getId(), request.recommendationId(), request.calendarItemId());

        // 1. Verify Recommendation Ownership & Status
        ContentRecommendation rec = recommendationRepository.findByIdAndUserId(request.recommendationId(), user.getId())
                .orElseThrow(() -> new ApiException("Recommendation not found or unauthorized", HttpStatus.NOT_FOUND, "RECOMMENDATION_NOT_FOUND"));

        if (rec.getStatus() != RecommendationStatus.VALIDATED && rec.getStatus() != RecommendationStatus.APPROVED) {
            throw new ApiException("Recommendation must be VALIDATED or APPROVED to generate production assets",
                    HttpStatus.BAD_REQUEST, "INVALID_RECOMMENDATION_STATUS");
        }

        // 2. Verify Calendar Item if provided
        CalendarItem calendarItem = null;
        if (request.calendarItemId() != null) {
            calendarItem = calendarItemRepository.findByIdAndUserId(request.calendarItemId(), user.getId())
                    .orElseThrow(() -> new ApiException("Calendar item not found or unauthorized", HttpStatus.NOT_FOUND, "CALENDAR_ITEM_NOT_FOUND"));

            if (calendarItem.getRecommendation() != null && !calendarItem.getRecommendation().getId().equals(rec.getId())) {
                throw new ApiException("Calendar item is not associated with this recommendation", HttpStatus.BAD_REQUEST, "CALENDAR_RECOMMENDATION_MISMATCH");
            }
        }

        GenerationMode genMode = request.mode() != null ? request.mode() : rec.getGenerationMode();

        // 3. Retrieve Evidence Context
        RecommendationContext context = evidenceRetrievalService.retrieveEvidence(user,
                RecommendationGenerateRequest.builder()
                        .topicId(rec.getTopic() != null ? rec.getTopic().getId() : null)
                        .contentType(rec.getContentType())
                        .mode(genMode)
                        .build());

        List<AiEvidenceItem> aiEvidenceItems;
        if (genMode == GenerationMode.BASELINE || context.evidenceItems() == null) {
            aiEvidenceItems = Collections.emptyList();
        } else {
            aiEvidenceItems = context.evidenceItems().stream()
                    .map(e -> AiEvidenceItem.builder()
                            .evidenceId(e.evidenceId())
                            .sourceType(e.sourceType().name())
                            .sourceId(e.sourceId() != null ? e.sourceId().toString() : "")
                            .summary(e.summary())
                            .relevanceScore(e.relevanceScore())
                            .metadata(e.metadata())
                            .build())
                    .collect(Collectors.toList());
        }


        List<String> requestedTypesStr = request.assetTypes() != null
                ? request.assetTypes().stream().map(Enum::name).collect(Collectors.toList())
                : List.of("CONTENT_BRIEF", "SCRIPT_OUTLINE", "HOOK", "TITLE_VARIATION", "CTA", "THUMBNAIL_PROMPT", "PRODUCTION_CHECKLIST");

        AiProductionGenerateRequest aiReq = AiProductionGenerateRequest.builder()
                .requestId(UUID.randomUUID().toString())
                .recommendationId(rec.getId().toString())
                .calendarItemId(calendarItem != null ? calendarItem.getId().toString() : null)
                .recommendationTitle(rec.getTitle())
                .recommendationAngle(rec.getAngle())
                .contentType(rec.getContentType())
                .platform(calendarItem != null ? calendarItem.getPlatform().name() : "YOUTUBE")
                .targetAudience(rec.getTargetAudience())
                .problemAddressed(rec.getProblemAddress())
                .keyPoints(rec.getKeyPoints())
                .topicName(rec.getTopic() != null ? rec.getTopic().getName() : null)
                .evidence(aiEvidenceItems)
                .mode(genMode.name())
                .requestedAssetTypes(requestedTypesStr)
                .build();

        // 4. Generate Production Draft from AI Microservice
        AiProductionGenerateResponse aiResp = aiServiceClient.generateProductionDraft(aiReq);
        ProductionDraftDto draft = mapToDraftDto(aiResp.draft());

        // 5. Validation Check (6 Guardrail Checks)
        ValidationResult validation = productionAssetValidator.validate(draft, context, genMode);
        boolean repairAttempted = false;
        Map<String, Object> repairResultJson = null;

        // 6. Repair Policy: Exactly ONE repair attempt on validation failure
        if (!validation.valid()) {
            log.warn("Production draft validation failed ({}). Attempting 1-shot repair...", validation.failureSummary());
            repairAttempted = true;

            List<Map<String, Object>> failedChecksMap = validation.checks().stream()
                    .filter(c -> !c.passed())
                    .map(c -> Map.<String, Object>of("name", c.checkName(), "reason", c.reason()))
                    .collect(Collectors.toList());

            AiProductionRepairRequest repReq = AiProductionRepairRequest.builder()
                    .requestId(UUID.randomUUID().toString())
                    .originalDraft(aiResp.draft())
                    .failedChecks(failedChecksMap)
                    .evidence(aiEvidenceItems)
                    .recommendationTitle(rec.getTitle())
                    .contentType(rec.getContentType())
                    .platform(calendarItem != null ? calendarItem.getPlatform().name() : "YOUTUBE")
                    .build();

            try {
                AiProductionRepairResponse repResp = aiServiceClient.repairProductionDraft(repReq);
                ProductionDraftDto repairedDraft = mapToDraftDto(repResp.repairedDraft());
                ValidationResult secondValidation = productionAssetValidator.validate(repairedDraft, context, genMode);

                repairResultJson = Map.of(
                        "explanation", repResp.repairExplanation() != null ? repResp.repairExplanation() : "",
                        "executionTimeMs", repResp.executionTimeMs() != null ? repResp.executionTimeMs() : 0.0,
                        "secondValidationPassed", secondValidation.valid()
                );

                if (secondValidation.valid()) {
                    log.info("1-shot repair SUCCEEDED for user={}, recId={}", user.getId(), rec.getId());
                    draft = repairedDraft;
                    validation = secondValidation;
                } else {
                    log.warn("1-shot repair FAILED second validation: {}", secondValidation.failureSummary());
                    validation = secondValidation;
                }
            } catch (Exception ex) {
                log.error("Error executing repair request: {}", ex.getMessage());
                repairResultJson = Map.of("error", ex.getMessage());
            }
        }

        ProductionAssetStatus initialStatus = validation.valid() ? ProductionAssetStatus.VALIDATED : ProductionAssetStatus.REJECTED;

        // Snapshot evidence context
        Map<String, Object> evidenceSnapshot = Map.of(
                "recommendationTitle", rec.getTitle(),
                "topicName", rec.getTopic() != null ? rec.getTopic().getName() : "General",
                "evidenceIds", draft != null && draft.evidenceIds() != null ? draft.evidenceIds() : Collections.emptyList(),
                "evidenceCount", draft != null && draft.evidenceIds() != null ? draft.evidenceIds().size() : 0
        );

        Map<String, Object> validationJson = Map.of(
                "valid", validation.valid(),
                "failureSummary", validation.failureSummary() != null ? validation.failureSummary() : "",
                "checks", validation.checks() != null ? validation.checks() : Collections.emptyList()
        );

        // Determine which asset types to persist
        Set<ProductionAssetType> typesToPersist = request.assetTypes() != null && !request.assetTypes().isEmpty()
                ? new HashSet<>(request.assetTypes())
                : Set.of(ProductionAssetType.values());

        List<ContentProductionAsset> savedAssets = new ArrayList<>();

        for (ProductionAssetType assetType : typesToPersist) {
            Map<String, Object> typeContent = extractAssetContent(draft, assetType);
            if (typeContent == null || typeContent.isEmpty()) {
                continue;
            }

            ContentProductionAsset asset = ContentProductionAsset.builder()
                    .user(user)
                    .recommendation(rec)
                    .calendarItem(calendarItem)
                    .assetType(assetType)
                    .status(initialStatus)
                    .generationMode(genMode)
                    .version(1)
                    .promptVersion(aiResp.promptVersion() != null ? aiResp.promptVersion() : "PRODUCTION_PROMPT_V1")
                    .modelName(aiResp.modelName() != null ? aiResp.modelName() : "pulsegpt-production-v1")
                    .contentJson(typeContent)
                    .evidenceSnapshot(evidenceSnapshot)
                    .validationJson(validationJson)
                    .revisionHistory(new ArrayList<>())
                    .repairAttempted(repairAttempted)
                    .repairResultJson(repairResultJson)
                    .build();

            ContentProductionAsset saved = productionAssetRepository.save(asset);
            savedAssets.add(saved);

            // Audit events
            auditService.logAuditEvent(user, "PRODUCTION_ASSET_GENERATED", "PRODUCTION_ASSET", saved.getId().toString(),
                    Map.of("assetType", assetType.name(), "recommendationId", rec.getId().toString(), "status", initialStatus.name()));

            if (validation.valid()) {
                auditService.logAuditEvent(user, "PRODUCTION_ASSET_VALIDATED", "PRODUCTION_ASSET", saved.getId().toString(),
                        Map.of("passed", true, "assetType", assetType.name()));
            }
            if (repairAttempted) {
                auditService.logAuditEvent(user, "PRODUCTION_ASSET_REPAIRED", "PRODUCTION_ASSET", saved.getId().toString(),
                        Map.of("repaired", validation.valid(), "assetType", assetType.name()));
            }
        }

        return savedAssets.stream().map(this::toResponseDto).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ProductionAssetResponse getAsset(User user, UUID assetId) {
        ContentProductionAsset asset = productionAssetRepository.findByIdAndUserId(assetId, user.getId())
                .orElseThrow(() -> new ApiException("Production asset not found or unauthorized", HttpStatus.NOT_FOUND, "ASSET_NOT_FOUND"));
        return toResponseDto(asset);
    }

    @Transactional(readOnly = true)
    public Page<ProductionAssetResponse> searchAssets(User user, UUID recommendationId, UUID calendarItemId,
                                                      ProductionAssetType assetType, ProductionAssetStatus status, Pageable pageable) {
        return productionAssetRepository.searchAssets(user.getId(), recommendationId, calendarItemId, assetType, status, pageable)
                .map(this::toResponseDto);
    }

    @Transactional
    public ProductionAssetResponse updateAsset(User user, UUID assetId, UpdateProductionAssetRequest updateRequest) {
        ContentProductionAsset asset = productionAssetRepository.findByIdAndUserId(assetId, user.getId())
                .orElseThrow(() -> new ApiException("Production asset not found or unauthorized", HttpStatus.NOT_FOUND, "ASSET_NOT_FOUND"));

        // Save current version into revision history
        List<Map<String, Object>> history = asset.getRevisionHistory() != null
                ? new ArrayList<>(asset.getRevisionHistory())
                : new ArrayList<>();

        Map<String, Object> historyEntry = Map.of(
                "version", asset.getVersion(),
                "contentJson", asset.getContentJson(),
                "status", asset.getStatus().name(),
                "modifiedAt", Instant.now().toString(),
                "modifiedBy", user.getId().toString()
        );
        history.add(historyEntry);

        // Apply edits
        Map<String, Object> newContent = updateRequest.contentJson() != null
                ? new HashMap<>(updateRequest.contentJson())
                : new HashMap<>(asset.getContentJson());

        if (updateRequest.title() != null) newContent.put("title", updateRequest.title());
        if (updateRequest.hook() != null) newContent.put("hook", updateRequest.hook());
        if (updateRequest.cta() != null) newContent.put("cta", updateRequest.cta());
        if (updateRequest.notes() != null) newContent.put("notes", updateRequest.notes());

        asset.setContentJson(newContent);
        asset.setRevisionHistory(history);
        asset.setVersion(asset.getVersion() + 1);
        asset.setStatus(ProductionAssetStatus.EDITED);

        ContentProductionAsset updated = productionAssetRepository.save(asset);

        auditService.logAuditEvent(user, "PRODUCTION_ASSET_EDITED", "PRODUCTION_ASSET", updated.getId().toString(),
                Map.of("version", updated.getVersion(), "assetType", updated.getAssetType().name()));

        return toResponseDto(updated);
    }

    @Transactional
    public ProductionAssetResponse approveAsset(User user, UUID assetId) {
        ContentProductionAsset asset = productionAssetRepository.findByIdAndUserId(assetId, user.getId())
                .orElseThrow(() -> new ApiException("Production asset not found or unauthorized", HttpStatus.NOT_FOUND, "ASSET_NOT_FOUND"));

        asset.setStatus(ProductionAssetStatus.APPROVED);
        asset.setApprovedAt(Instant.now());
        asset.setApprovedBy(user.getId());

        ContentProductionAsset approved = productionAssetRepository.save(asset);

        auditService.logAuditEvent(user, "PRODUCTION_ASSET_APPROVED", "PRODUCTION_ASSET", approved.getId().toString(),
                Map.of("assetType", approved.getAssetType().name(), "recommendationId", approved.getRecommendation().getId().toString()));

        return toResponseDto(approved);
    }

    @Transactional
    public ProductionAssetResponse archiveAsset(User user, UUID assetId) {
        ContentProductionAsset asset = productionAssetRepository.findByIdAndUserId(assetId, user.getId())
                .orElseThrow(() -> new ApiException("Production asset not found or unauthorized", HttpStatus.NOT_FOUND, "ASSET_NOT_FOUND"));

        asset.setStatus(ProductionAssetStatus.ARCHIVED);
        ContentProductionAsset archived = productionAssetRepository.save(asset);

        auditService.logAuditEvent(user, "PRODUCTION_ASSET_ARCHIVED", "PRODUCTION_ASSET", archived.getId().toString(),
                Map.of("assetType", archived.getAssetType().name()));

        return toResponseDto(archived);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getAssetEvidence(User user, UUID assetId) {
        ContentProductionAsset asset = productionAssetRepository.findByIdAndUserId(assetId, user.getId())
                .orElseThrow(() -> new ApiException("Production asset not found or unauthorized", HttpStatus.NOT_FOUND, "ASSET_NOT_FOUND"));

        return asset.getEvidenceSnapshot() != null ? asset.getEvidenceSnapshot() : Collections.emptyMap();
    }

    // -------------------------------------------------------------------------
    // Helper Mappings
    // -------------------------------------------------------------------------

    private ProductionDraftDto mapToDraftDto(Map<String, Object> draftMap) {
        if (draftMap == null) return null;
        try {
            return objectMapper.convertValue(draftMap, ProductionDraftDto.class);
        } catch (Exception e) {
            log.error("Failed to deserialize production draft map: {}", e.getMessage());
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> extractAssetContent(ProductionDraftDto draft, ProductionAssetType type) {
        if (draft == null) return Collections.emptyMap();

        try {
            switch (type) {
                case CONTENT_BRIEF:
                    return draft.brief() != null ? objectMapper.convertValue(draft.brief(), Map.class) : Collections.emptyMap();
                case SCRIPT_OUTLINE:
                    return draft.outline() != null ? objectMapper.convertValue(draft.outline(), Map.class) : Collections.emptyMap();
                case HOOK:
                    return draft.hooks() != null ? Map.of("hooks", draft.hooks()) : Collections.emptyMap();
                case TITLE_VARIATION:
                    return draft.titles() != null ? Map.of("titles", draft.titles()) : Collections.emptyMap();
                case CTA:
                    return draft.ctas() != null ? Map.of("ctas", draft.ctas()) : Collections.emptyMap();
                case THUMBNAIL_PROMPT:
                    return draft.thumbnail() != null ? objectMapper.convertValue(draft.thumbnail(), Map.class) : Collections.emptyMap();
                case PRODUCTION_CHECKLIST:
                    return draft.checklist() != null ? objectMapper.convertValue(draft.checklist(), Map.class) : Collections.emptyMap();
                default:
                    return Collections.emptyMap();
            }
        } catch (Exception e) {
            log.error("Error extracting asset content for type {}: {}", type, e.getMessage());
            return Collections.emptyMap();
        }
    }

    @SuppressWarnings("unchecked")
    private ProductionAssetResponse toResponseDto(ContentProductionAsset asset) {
        List<String> evidenceIds = Collections.emptyList();
        if (asset.getEvidenceSnapshot() != null && asset.getEvidenceSnapshot().get("evidenceIds") instanceof List<?> list) {
            evidenceIds = (List<String>) list;
        }

        return ProductionAssetResponse.builder()
                .id(asset.getId())
                .userId(asset.getUser().getId())
                .recommendationId(asset.getRecommendation().getId())
                .recommendationTitle(asset.getRecommendation().getTitle())
                .calendarItemId(asset.getCalendarItem() != null ? asset.getCalendarItem().getId() : null)
                .assetType(asset.getAssetType())
                .status(asset.getStatus())
                .generationMode(asset.getGenerationMode())
                .version(asset.getVersion())
                .promptVersion(asset.getPromptVersion())
                .modelName(asset.getModelName())
                .contentJson(asset.getContentJson())
                .evidenceSnapshot(asset.getEvidenceSnapshot())
                .evidenceIds(evidenceIds)
                .validationJson(asset.getValidationJson())
                .revisionHistory(asset.getRevisionHistory())
                .repairAttempted(asset.getRepairAttempted())
                .repairResultJson(asset.getRepairResultJson())
                .approvedAt(asset.getApprovedAt())
                .approvedBy(asset.getApprovedBy())
                .createdAt(asset.getCreatedAt())
                .updatedAt(asset.getUpdatedAt())
                .build();
    }
}
