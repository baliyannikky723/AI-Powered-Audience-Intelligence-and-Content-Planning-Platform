package com.pulsegpt;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pulsegpt.audit.AuditService;
import com.pulsegpt.common.exception.ApiException;
import com.pulsegpt.export.ContentExport;
import com.pulsegpt.export.ContentExportRepository;
import com.pulsegpt.export.ExportType;
import com.pulsegpt.export.dto.ExportRequest;
import com.pulsegpt.export.dto.ExportResponse;
import com.pulsegpt.export.service.ContentExportService;
import com.pulsegpt.export.service.ExportFormattingService;
import com.pulsegpt.production.ContentProductionAsset;
import com.pulsegpt.production.ContentProductionAssetRepository;
import com.pulsegpt.production.ProductionAssetStatus;
import com.pulsegpt.production.ProductionAssetType;
import com.pulsegpt.recommendation.ContentRecommendation;
import com.pulsegpt.recommendation.GenerationMode;
import com.pulsegpt.user.User;
import com.pulsegpt.user.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Phase 3L — ContentExportService Unit Tests")
class ContentExportServiceTest {

    @Mock
    private ContentExportRepository exportRepository;

    @Mock
    private ContentProductionAssetRepository productionAssetRepository;

    @Mock
    private ExportFormattingService formattingService;

    @Mock
    private AuditService auditService;

    private ContentExportService exportService;

    private User creatorUser;
    private User otherUser;
    private ContentRecommendation recommendation;
    private ContentProductionAsset approvedAsset;
    private ContentProductionAsset unapprovedAsset;

    @BeforeEach
    void setUp() {
        exportService = new ContentExportService(exportRepository, productionAssetRepository, formattingService, auditService);

        creatorUser = User.builder()
                .id(UUID.randomUUID())
                .name("Alex Rivera")
                .email("alex@creators.com")
                .role(UserRole.CREATOR)
                .build();

        otherUser = User.builder()
                .id(UUID.randomUUID())
                .name("Bianca Chen")
                .email("bianca@creators.com")
                .role(UserRole.CREATOR)
                .build();

        recommendation = ContentRecommendation.builder()
                .id(UUID.randomUUID())
                .user(creatorUser)
                .title("Optimizing PostgreSQL Query Plans")
                .build();

        approvedAsset = ContentProductionAsset.builder()
                .id(UUID.randomUUID())
                .user(creatorUser)
                .recommendation(recommendation)
                .assetType(ProductionAssetType.CONTENT_BRIEF)
                .status(ProductionAssetStatus.APPROVED)
                .generationMode(GenerationMode.EVIDENCE_GROUNDED)
                .version(1)
                .contentJson(Map.of("title", "Optimizing PostgreSQL Query Plans"))
                .approvedAt(Instant.now())
                .build();

        unapprovedAsset = ContentProductionAsset.builder()
                .id(UUID.randomUUID())
                .user(creatorUser)
                .recommendation(recommendation)
                .assetType(ProductionAssetType.CONTENT_BRIEF)
                .status(ProductionAssetStatus.GENERATED)
                .generationMode(GenerationMode.EVIDENCE_GROUNDED)
                .version(1)
                .contentJson(Map.of("title", "Optimizing PostgreSQL Query Plans"))
                .build();
    }

    @Test
    @DisplayName("Approved production asset exports successfully and emits audit event")
    void testApprovedAssetCanExport() {
        when(productionAssetRepository.findByIdAndUserId(approvedAsset.getId(), creatorUser.getId()))
                .thenReturn(Optional.of(approvedAsset));
        when(productionAssetRepository.findAllByUserIdAndRecommendationId(creatorUser.getId(), recommendation.getId()))
                .thenReturn(List.of(approvedAsset));

        byte[] fakeData = "# Markdown Export Content".getBytes(StandardCharsets.UTF_8);
        when(formattingService.formatExport(eq(ExportType.MARKDOWN), eq(approvedAsset), any()))
                .thenReturn(fakeData);

        when(exportRepository.save(any(ContentExport.class)))
                .thenAnswer(inv -> {
                    ContentExport e = inv.getArgument(0);
                    e.setId(UUID.randomUUID());
                    e.setCreatedAt(Instant.now());
                    return e;
                });

        ExportRequest request = new ExportRequest(ExportType.MARKDOWN);
        ExportResponse response = exportService.createExport(creatorUser, approvedAsset.getId(), request);

        assertThat(response).isNotNull();
        assertThat(response.exportType()).isEqualTo(ExportType.MARKDOWN);
        assertThat(response.contentHash()).isNotBlank();
        assertThat(response.downloadUrl()).contains("/download");

        verify(auditService).logAuditEvent(eq(creatorUser), eq("PRODUCTION_EXPORT_CREATED"), eq("CONTENT_EXPORT"), any(), any());
    }

    @Test
    @DisplayName("Unapproved production asset cannot be exported and returns HTTP 400")
    void testUnapprovedAssetCannotExport() {
        when(productionAssetRepository.findByIdAndUserId(unapprovedAsset.getId(), creatorUser.getId()))
                .thenReturn(Optional.of(unapprovedAsset));

        ExportRequest request = new ExportRequest(ExportType.MARKDOWN);

        assertThatThrownBy(() -> exportService.createExport(creatorUser, unapprovedAsset.getId(), request))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Only creator-approved production assets can be exported")
                .extracting(e -> ((ApiException) e).getStatus())
                .isEqualTo(HttpStatus.BAD_REQUEST);

        verifyNoInteractions(formattingService);
        verifyNoInteractions(exportRepository);
    }

    @Test
    @DisplayName("Cross-user export attempt returns HTTP 404")
    void testCrossUserExportReturns404() {
        when(productionAssetRepository.findByIdAndUserId(approvedAsset.getId(), otherUser.getId()))
                .thenReturn(Optional.empty());

        ExportRequest request = new ExportRequest(ExportType.PDF);

        assertThatThrownBy(() -> exportService.createExport(otherUser, approvedAsset.getId(), request))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Production asset not found or unauthorized")
                .extracting(e -> ((ApiException) e).getStatus())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @DisplayName("Production Package creates ZIP export successfully")
    void testCreateProductionPackageSuccess() {
        when(productionAssetRepository.findByIdAndUserId(approvedAsset.getId(), creatorUser.getId()))
                .thenReturn(Optional.of(approvedAsset));
        when(productionAssetRepository.findAllByUserIdAndRecommendationId(creatorUser.getId(), recommendation.getId()))
                .thenReturn(List.of(approvedAsset));

        byte[] fakeZip = "PK-ZIP-BYTES".getBytes(StandardCharsets.UTF_8);
        when(formattingService.formatExport(eq(ExportType.PRODUCTION_PACKAGE), eq(approvedAsset), any()))
                .thenReturn(fakeZip);

        when(exportRepository.save(any(ContentExport.class)))
                .thenAnswer(inv -> {
                    ContentExport e = inv.getArgument(0);
                    e.setId(UUID.randomUUID());
                    e.setCreatedAt(Instant.now());
                    return e;
                });

        ExportResponse response = exportService.createProductionPackage(creatorUser, approvedAsset.getId());

        assertThat(response.exportType()).isEqualTo(ExportType.PRODUCTION_PACKAGE);
        assertThat(response.fileName()).endsWith("-package.zip");
        assertThat(response.mimeType()).isEqualTo("application/zip");
    }

    @Test
    @DisplayName("Download export returns valid resource and logs audit event")
    void testDownloadExportSuccess() {
        UUID exportId = UUID.randomUUID();
        byte[] expectedData = "SAMPLE EXPORT DATA".getBytes(StandardCharsets.UTF_8);
        String expectedHash = ContentExportService.computeSha256(expectedData);

        ContentExport export = ContentExport.builder()
                .id(exportId)
                .user(creatorUser)
                .productionAsset(approvedAsset)
                .exportType(ExportType.MARKDOWN)
                .fileName("pulsegpt-production-v1.md")
                .mimeType("text/markdown")
                .contentHash(expectedHash)
                .contentData(expectedData)
                .build();

        when(exportRepository.findByIdAndUserId(exportId, creatorUser.getId()))
                .thenReturn(Optional.of(export));

        ContentExportService.DownloadResource resource = exportService.downloadExport(creatorUser, exportId);

        assertThat(resource.data()).isEqualTo(expectedData);
        assertThat(resource.fileName()).isEqualTo("pulsegpt-production-v1.md");
        assertThat(resource.mimeType()).isEqualTo("text/markdown");
        assertThat(resource.contentHash()).isEqualTo(expectedHash);

        verify(auditService).logAuditEvent(eq(creatorUser), eq("PRODUCTION_EXPORT_DOWNLOADED"), eq("CONTENT_EXPORT"), eq(exportId.toString()), any());
    }

    @Test
    @DisplayName("List exports returns paginated history for asset")
    void testListExportsForAsset() {
        when(productionAssetRepository.findByIdAndUserId(approvedAsset.getId(), creatorUser.getId()))
                .thenReturn(Optional.of(approvedAsset));

        ContentExport export1 = ContentExport.builder()
                .id(UUID.randomUUID())
                .user(creatorUser)
                .productionAsset(approvedAsset)
                .exportType(ExportType.MARKDOWN)
                .fileName("file1.md")
                .mimeType("text/markdown")
                .contentHash("hash1")
                .version(1)
                .createdAt(Instant.now())
                .build();

        Pageable pageable = PageRequest.of(0, 10);
        when(exportRepository.findAllByUserIdAndProductionAssetId(creatorUser.getId(), approvedAsset.getId(), pageable))
                .thenReturn(new PageImpl<>(List.of(export1), pageable, 1));

        Page<ExportResponse> result = exportService.listExportsForAsset(creatorUser, approvedAsset.getId(), pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).exportType()).isEqualTo(ExportType.MARKDOWN);
    }
}
