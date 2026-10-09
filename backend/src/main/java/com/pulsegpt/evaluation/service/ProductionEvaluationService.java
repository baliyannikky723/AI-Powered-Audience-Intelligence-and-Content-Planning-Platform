package com.pulsegpt.evaluation.service;

import com.pulsegpt.evaluation.EvaluationRecord;
import com.pulsegpt.evaluation.EvaluationRecordRepository;
import com.pulsegpt.production.ContentProductionAsset;
import com.pulsegpt.production.ContentProductionAssetRepository;
import com.pulsegpt.production.ProductionAssetStatus;
import com.pulsegpt.user.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductionEvaluationService {

    private final ContentProductionAssetRepository productionAssetRepository;
    private final EvaluationRecordRepository evaluationRecordRepository;

    @Transactional(readOnly = true)
    public Map<String, Object> evaluateProductionAsset(UUID assetId, User user) {
        ContentProductionAsset asset = productionAssetRepository.findByIdAndUserId(assetId, user.getId())
                .orElseThrow(() -> new IllegalArgumentException("Production asset not found: " + assetId));

        Map<String, Object> valJson = asset.getValidationJson();
        boolean validationPassed = valJson != null && Boolean.TRUE.equals(valJson.get("valid"));
        boolean repairUsed = asset.getVersion() > 1;
        boolean repairSucceeded = repairUsed && validationPassed;
        boolean creatorEdited = ProductionAssetStatus.EDITED.equals(asset.getStatus());
        boolean approved = ProductionAssetStatus.APPROVED.equals(asset.getStatus());
        int revisionCount = asset.getVersion() != null ? asset.getVersion() : 1;

        Long timeToApprovalMs = null;
        if (asset.getApprovedAt() != null && asset.getCreatedAt() != null) {
            timeToApprovalMs = Duration.between(asset.getCreatedAt(), asset.getApprovedAt()).toMillis();
        }

        Map<String, Object> result = new HashMap<>();
        result.put("assetId", asset.getId().toString());
        result.put("assetType", asset.getAssetType() != null ? asset.getAssetType().name() : "CONTENT_BRIEF");
        result.put("generationMode", asset.getGenerationMode() != null ? asset.getGenerationMode().name() : "EVIDENCE_GROUNDED");
        result.put("validationPassed", validationPassed);
        result.put("repairUsed", repairUsed);
        result.put("repairSucceeded", repairSucceeded);
        result.put("creatorEdited", creatorEdited);
        result.put("approved", approved);
        result.put("revisionCount", revisionCount);
        result.put("timeToApprovalMs", timeToApprovalMs);

        return result;
    }

    @Transactional
    public EvaluationRecord recordProductionEvaluation(ContentProductionAsset asset, User user, Map<String, Object> metrics) {
        EvaluationRecord record = EvaluationRecord.builder()
                .user(user)
                .targetType("PRODUCTION")
                .targetId(asset.getId())
                .generationMode(asset.getGenerationMode() != null ? asset.getGenerationMode().name() : "EVIDENCE_GROUNDED")
                .metrics(metrics)
                .build();
        return evaluationRecordRepository.save(record);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getAggregatedProductionMetrics(UUID userId) {
        List<ContentProductionAsset> list = productionAssetRepository.findAll().stream()
                .filter(a -> a.getUser() != null && a.getUser().getId().equals(userId))
                .toList();

        if (list.isEmpty()) {
            return Map.of(
                    "totalAssets", 0,
                    "approvedCount", 0,
                    "approvalRate", 0.0,
                    "editedCount", 0,
                    "creatorEditRate", 0.0,
                    "avgRevisionCount", 0.0
            );
        }

        int approved = 0;
        int edited = 0;
        int totalRevisions = 0;

        for (ContentProductionAsset a : list) {
            if (ProductionAssetStatus.APPROVED.equals(a.getStatus())) approved++;
            if (ProductionAssetStatus.EDITED.equals(a.getStatus())) edited++;
            totalRevisions += (a.getVersion() != null ? a.getVersion() : 1);
        }

        return Map.of(
                "totalAssets", list.size(),
                "approvedCount", approved,
                "approvalRate", (double) approved / list.size(),
                "editedCount", edited,
                "creatorEditRate", (double) edited / list.size(),
                "avgRevisionCount", (double) totalRevisions / list.size()
        );
    }
}
