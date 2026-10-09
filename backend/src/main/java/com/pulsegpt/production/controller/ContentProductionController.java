package com.pulsegpt.production.controller;

import com.pulsegpt.common.ApiResponse;
import com.pulsegpt.common.dto.PageResponse;
import com.pulsegpt.production.ProductionAssetStatus;
import com.pulsegpt.production.ProductionAssetType;
import com.pulsegpt.production.dto.ProductionAssetResponse;
import com.pulsegpt.production.dto.ProductionGenerateRequest;
import com.pulsegpt.production.dto.UpdateProductionAssetRequest;
import com.pulsegpt.production.service.ContentProductionService;
import com.pulsegpt.security.CurrentUserService;
import com.pulsegpt.user.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/production-assets")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Content Production Copilot", description = "Endpoints for AI-assisted content production drafts, creator editing, approval, and versioning")
public class ContentProductionController {

    private final ContentProductionService productionService;
    private final CurrentUserService currentUserService;

    @PostMapping("/generate")
    @Operation(summary = "Generate structured production assets (brief, outline, hooks, titles, CTAs, thumbnail prompt, checklist) from an approved/validated recommendation")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Production assets generated and persisted"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid generation request or unapproved recommendation"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Recommendation or calendar item not found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "503", description = "AI service unavailable")
    })
    public ResponseEntity<ApiResponse<List<ProductionAssetResponse>>> generateProductionAssets(
            @Valid @RequestBody ProductionGenerateRequest request
    ) {
        User user = currentUserService.requireUser();
        List<ProductionAssetResponse> assets = productionService.generateProductionAssets(user, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Production assets generated successfully", assets));
    }

    @GetMapping
    @Operation(summary = "Get paginated production assets with optional filters for recommendation, calendar item, asset type, and status")
    public ResponseEntity<ApiResponse<PageResponse<ProductionAssetResponse>>> searchAssets(
            @RequestParam(required = false) UUID recommendationId,
            @RequestParam(required = false) UUID calendarItemId,
            @RequestParam(required = false) ProductionAssetType assetType,
            @RequestParam(required = false) ProductionAssetStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDir
    ) {
        User user = currentUserService.requireUser();
        int safeSize = Math.min(Math.max(size, 1), 100);
        Sort.Direction direction = "ASC".equalsIgnoreCase(sortDir) ? Sort.Direction.ASC : Sort.Direction.DESC;
        String safeSortBy = "updatedAt".equalsIgnoreCase(sortBy) ? "updatedAt" : "createdAt";
        Pageable pageable = PageRequest.of(page, safeSize, Sort.by(direction, safeSortBy));

        Page<ProductionAssetResponse> assetPage = productionService.searchAssets(user, recommendationId, calendarItemId, assetType, status, pageable);
        return ResponseEntity.ok(ApiResponse.ok(PageResponse.from(assetPage)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a specific production asset by ID")
    public ResponseEntity<ApiResponse<ProductionAssetResponse>> getAsset(@PathVariable UUID id) {
        User user = currentUserService.requireUser();
        ProductionAssetResponse asset = productionService.getAsset(user, id);
        return ResponseEntity.ok(ApiResponse.ok(asset));
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Edit a production asset draft (creator editing with revision history tracking)")
    public ResponseEntity<ApiResponse<ProductionAssetResponse>> updateAsset(
            @PathVariable UUID id,
            @RequestBody UpdateProductionAssetRequest updateRequest
    ) {
        User user = currentUserService.requireUser();
        ProductionAssetResponse updated = productionService.updateAsset(user, id, updateRequest);
        return ResponseEntity.ok(ApiResponse.ok("Production asset updated successfully", updated));
    }

    @PostMapping("/{id}/approve")
    @Operation(summary = "Creator approval of production asset draft (does not auto-publish)")
    public ResponseEntity<ApiResponse<ProductionAssetResponse>> approveAsset(@PathVariable UUID id) {
        User user = currentUserService.requireUser();
        ProductionAssetResponse approved = productionService.approveAsset(user, id);
        return ResponseEntity.ok(ApiResponse.ok("Production asset approved by creator", approved));
    }

    @PostMapping("/{id}/archive")
    @Operation(summary = "Archive a production asset")
    public ResponseEntity<ApiResponse<ProductionAssetResponse>> archiveAsset(@PathVariable UUID id) {
        User user = currentUserService.requireUser();
        ProductionAssetResponse archived = productionService.archiveAsset(user, id);
        return ResponseEntity.ok(ApiResponse.ok("Production asset archived successfully", archived));
    }

    @GetMapping("/{id}/evidence")
    @Operation(summary = "Retrieve evidence snapshot and verified source IDs for a production asset")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getAssetEvidence(@PathVariable UUID id) {
        User user = currentUserService.requireUser();
        Map<String, Object> evidence = productionService.getAssetEvidence(user, id);
        return ResponseEntity.ok(ApiResponse.ok(evidence));
    }
}
