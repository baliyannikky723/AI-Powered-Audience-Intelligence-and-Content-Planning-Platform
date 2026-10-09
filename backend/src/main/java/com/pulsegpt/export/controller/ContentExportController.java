package com.pulsegpt.export.controller;

import com.pulsegpt.common.ApiResponse;
import com.pulsegpt.common.dto.PageResponse;
import com.pulsegpt.export.dto.ExportRequest;
import com.pulsegpt.export.dto.ExportResponse;
import com.pulsegpt.export.service.ContentExportService;
import com.pulsegpt.security.CurrentUserService;
import com.pulsegpt.user.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Content Exporter & Production Packager", description = "Endpoints for multi-format creator exports (Markdown, PDF, Teleprompter, ZIP, JSON)")
public class ContentExportController {

    private final ContentExportService exportService;
    private final CurrentUserService currentUserService;

    @PostMapping("/production-assets/{id}/exports")
    @Operation(summary = "Export an approved production asset to a specific format (Markdown, PDF, Teleprompter, Checklist, JSON, Timeline)")
    public ResponseEntity<ApiResponse<ExportResponse>> createExport(
            @PathVariable UUID id,
            @Valid @RequestBody ExportRequest request
    ) {
        User user = currentUserService.requireUser();
        ExportResponse response = exportService.createExport(user, id, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Export generated successfully", response));
    }

    @PostMapping("/production-assets/{id}/package")
    @Operation(summary = "Generate a comprehensive production package ZIP archive containing all asset files, teleprompter script, PDF pack, and README")
    public ResponseEntity<ApiResponse<ExportResponse>> createProductionPackage(
            @PathVariable UUID id
    ) {
        User user = currentUserService.requireUser();
        ExportResponse response = exportService.createProductionPackage(user, id);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Production package created successfully", response));
    }

    @GetMapping("/production-assets/{id}/exports")
    @Operation(summary = "List previous exports and packages generated for a production asset")
    public ResponseEntity<ApiResponse<PageResponse<ExportResponse>>> listExports(
            @PathVariable UUID id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDir
    ) {
        User user = currentUserService.requireUser();
        int safeSize = Math.min(Math.max(size, 1), 100);
        Sort.Direction direction = "ASC".equalsIgnoreCase(sortDir) ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(page, safeSize, Sort.by(direction, "createdAt"));

        Page<ExportResponse> exportPage = exportService.listExportsForAsset(user, id, pageable);
        return ResponseEntity.ok(ApiResponse.ok(PageResponse.from(exportPage)));
    }

    @GetMapping("/production-exports/{exportId}")
    @Operation(summary = "Get metadata and SHA-256 hash for a specific export record")
    public ResponseEntity<ApiResponse<ExportResponse>> getExport(
            @PathVariable UUID exportId
    ) {
        User user = currentUserService.requireUser();
        ExportResponse response = exportService.getExport(user, exportId);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping("/production-exports/{exportId}/download")
    @Operation(summary = "Download the generated export file with SHA-256 integrity verification header")
    public ResponseEntity<byte[]> downloadExport(
            @PathVariable UUID exportId
    ) {
        User user = currentUserService.requireUser();
        ContentExportService.DownloadResource resource = exportService.downloadExport(user, exportId);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(resource.mimeType()));
        headers.setContentDispositionFormData("attachment", resource.fileName());
        headers.setContentLength(resource.data().length);
        headers.set("X-Content-Hash", resource.contentHash());
        headers.set("Access-Control-Expose-Headers", "Content-Disposition, X-Content-Hash");

        return ResponseEntity.ok()
                .headers(headers)
                .body(resource.data());
    }
}
