package com.pulsegpt.export.service;

import com.pulsegpt.audit.AuditService;
import com.pulsegpt.common.exception.ApiException;
import com.pulsegpt.export.ContentExport;
import com.pulsegpt.export.ContentExportRepository;
import com.pulsegpt.export.ExportType;
import com.pulsegpt.export.dto.ExportRequest;
import com.pulsegpt.export.dto.ExportResponse;
import com.pulsegpt.production.ContentProductionAsset;
import com.pulsegpt.production.ContentProductionAssetRepository;
import com.pulsegpt.production.ProductionAssetStatus;
import com.pulsegpt.user.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ContentExportService {

    private final ContentExportRepository exportRepository;
    private final ContentProductionAssetRepository productionAssetRepository;
    private final ExportFormattingService formattingService;
    private final AuditService auditService;

    public record DownloadResource(
            byte[] data,
            String fileName,
            String mimeType,
            String contentHash
    ) {}

    @Transactional
    public ExportResponse createExport(User user, UUID assetId, ExportRequest request) {
        ContentProductionAsset asset = getAndValidateAsset(user, assetId);
        List<ContentProductionAsset> allAssets = productionAssetRepository
                .findAllByUserIdAndRecommendationId(user.getId(), asset.getRecommendation().getId());

        long startTime = System.currentTimeMillis();
        byte[] contentData = formattingService.formatExport(request.exportType(), asset, allAssets);
        long latencyMs = System.currentTimeMillis() - startTime;

        String contentHash = computeSha256(contentData);
        String safeFileName = generateSafeFileName(asset, request.exportType());

        Map<String, Object> metadata = Map.of(
                "latencyMs", latencyMs,
                "generationMode", asset.getGenerationMode().name(),
                "modelName", asset.getModelName() != null ? asset.getModelName() : "pulsegpt-v1",
                "promptVersion", asset.getPromptVersion() != null ? asset.getPromptVersion() : "PRODUCTION_PROMPT_V1",
                "sourceRecommendationId", asset.getRecommendation().getId().toString(),
                "exportTimestamp", Instant.now().toString()
        );

        ContentExport export = ContentExport.builder()
                .user(user)
                .productionAsset(asset)
                .exportType(request.exportType())
                .fileName(safeFileName)
                .mimeType(request.exportType().getMimeType())
                .contentHash(contentHash)
                .version(asset.getVersion())
                .fileSizeBytes((long) contentData.length)
                .metadataJson(metadata)
                .contentData(contentData)
                .build();

        ContentExport saved = exportRepository.save(export);

        auditService.logAuditEvent(user, "PRODUCTION_EXPORT_CREATED", "CONTENT_EXPORT", saved.getId().toString(),
                Map.of("exportType", request.exportType().name(), "contentHash", contentHash, "version", asset.getVersion()));

        return toResponseDto(saved);
    }

    @Transactional
    public ExportResponse createProductionPackage(User user, UUID assetId) {
        return createExport(user, assetId, new ExportRequest(ExportType.PRODUCTION_PACKAGE));
    }

    @Transactional(readOnly = true)
    public Page<ExportResponse> listExportsForAsset(User user, UUID assetId, Pageable pageable) {
        getAndValidateAsset(user, assetId);
        return exportRepository.findAllByUserIdAndProductionAssetId(user.getId(), assetId, pageable)
                .map(this::toResponseDto);
    }

    @Transactional(readOnly = true)
    public ExportResponse getExport(User user, UUID exportId) {
        ContentExport export = exportRepository.findByIdAndUserId(exportId, user.getId())
                .orElseThrow(() -> new ApiException("Export record not found or unauthorized", HttpStatus.NOT_FOUND, "EXPORT_NOT_FOUND"));
        return toResponseDto(export);
    }

    @Transactional
    public DownloadResource downloadExport(User user, UUID exportId) {
        ContentExport export = exportRepository.findByIdAndUserId(exportId, user.getId())
                .orElseThrow(() -> new ApiException("Export record not found or unauthorized", HttpStatus.NOT_FOUND, "EXPORT_NOT_FOUND"));

        byte[] data = export.getContentData();
        if (data == null || data.length == 0) {
            // Deterministic re-generation if binary data was pruned
            ContentProductionAsset asset = export.getProductionAsset();
            List<ContentProductionAsset> allAssets = productionAssetRepository
                    .findAllByUserIdAndRecommendationId(user.getId(), asset.getRecommendation().getId());
            data = formattingService.formatExport(export.getExportType(), asset, allAssets);
        }

        auditService.logAuditEvent(user, "PRODUCTION_EXPORT_DOWNLOADED", "CONTENT_EXPORT", export.getId().toString(),
                Map.of("exportType", export.getExportType().name(), "contentHash", export.getContentHash(), "fileName", export.getFileName()));

        return new DownloadResource(data, export.getFileName(), export.getMimeType(), export.getContentHash());
    }

    private ContentProductionAsset getAndValidateAsset(User user, UUID assetId) {
        ContentProductionAsset asset = productionAssetRepository.findByIdAndUserId(assetId, user.getId())
                .orElseThrow(() -> new ApiException("Production asset not found or unauthorized", HttpStatus.NOT_FOUND, "ASSET_NOT_FOUND"));

        if (asset.getStatus() != ProductionAssetStatus.APPROVED) {
            log.warn("Export rejected for user={} assetId={}: status={}", user.getId(), assetId, asset.getStatus());
            throw new ApiException("Only creator-approved production assets can be exported. Current status: " + asset.getStatus(),
                    HttpStatus.BAD_REQUEST, "ASSET_NOT_APPROVED");
        }

        return asset;
    }

    private String generateSafeFileName(ContentProductionAsset asset, ExportType exportType) {
        String base = "pulsegpt-production-" + asset.getId().toString().substring(0, 8) + "-v" + asset.getVersion();
        return base + exportType.getDefaultExtension();
    }

    public static String computeSha256(byte[] data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(data != null ? data : new byte[0]);
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not found", e);
        }
    }

    private ExportResponse toResponseDto(ContentExport export) {
        return ExportResponse.builder()
                .id(export.getId())
                .productionAssetId(export.getProductionAsset().getId())
                .recommendationId(export.getProductionAsset().getRecommendation().getId())
                .exportType(export.getExportType())
                .fileName(export.getFileName())
                .mimeType(export.getMimeType())
                .contentHash(export.getContentHash())
                .version(export.getVersion())
                .fileSizeBytes(export.getFileSizeBytes())
                .createdAt(export.getCreatedAt())
                .downloadUrl("/production-exports/" + export.getId() + "/download")
                .metadata(export.getMetadataJson())
                .build();
    }
}
