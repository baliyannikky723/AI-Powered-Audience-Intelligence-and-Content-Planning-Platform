package com.pulsegpt;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pulsegpt.ai.client.AiServiceClient;
import com.pulsegpt.audit.AuditLogRepository;
import com.pulsegpt.export.ContentExport;
import com.pulsegpt.export.ContentExportRepository;
import com.pulsegpt.export.ExportType;
import com.pulsegpt.export.dto.ExportRequest;
import com.pulsegpt.production.ContentProductionAsset;
import com.pulsegpt.production.ContentProductionAssetRepository;
import com.pulsegpt.production.ProductionAssetStatus;
import com.pulsegpt.production.ProductionAssetType;
import com.pulsegpt.recommendation.ContentRecommendation;
import com.pulsegpt.recommendation.GenerationMode;
import com.pulsegpt.recommendation.service.EvidenceRetrievalService;
import com.pulsegpt.security.JwtService;
import com.pulsegpt.user.User;
import com.pulsegpt.user.UserRepository;
import com.pulsegpt.user.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import javax.sql.DataSource;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.time.Instant;
import java.util.*;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Phase 3L — Multi-Format Content Exporter Integration Tests")
class ContentExportIntegrationTest {

    @TestConfiguration
    static class TestDataSourceConfig {
        @Bean
        @Primary
        public DataSource dataSource() throws SQLException {
            DataSource ds = mock(DataSource.class);
            Connection conn = mock(Connection.class);
            DatabaseMetaData metaData = mock(DatabaseMetaData.class);

            when(ds.getConnection()).thenReturn(conn);
            when(ds.getConnection(any(), any())).thenReturn(conn);
            when(conn.getMetaData()).thenReturn(metaData);
            when(metaData.getConnection()).thenReturn(conn);
            when(metaData.getDatabaseProductName()).thenReturn("PostgreSQL");
            when(metaData.getDatabaseProductVersion()).thenReturn("16.0");
            when(metaData.getDatabaseMajorVersion()).thenReturn(16);
            when(metaData.getDatabaseMinorVersion()).thenReturn(0);
            when(metaData.getDriverName()).thenReturn("PostgreSQL JDBC Driver");
            when(metaData.getDriverVersion()).thenReturn("42.7.2");
            when(metaData.getDriverMajorVersion()).thenReturn(42);
            when(metaData.getDriverMinorVersion()).thenReturn(7);
            return ds;
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtService jwtService;

    @MockitoBean
    private AiServiceClient aiServiceClient;

    @MockitoBean
    private EvidenceRetrievalService evidenceRetrievalService;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private ContentProductionAssetRepository productionAssetRepository;

    @MockitoBean
    private ContentExportRepository exportRepository;

    @MockitoBean
    private AuditLogRepository auditLogRepository;

    private User creatorA;
    private User creatorB;
    private String tokenA;
    private String tokenB;
    private ContentRecommendation recommendationA;
    private ContentProductionAsset approvedAsset;
    private ContentProductionAsset unapprovedAsset;

    @BeforeEach
    void setUp() {
        creatorA = User.builder()
                .id(UUID.randomUUID())
                .name("Alex Rivera")
                .email("alex@creators.com")
                .passwordHash("hashed")
                .role(UserRole.CREATOR)
                .active(true)
                .build();

        creatorB = User.builder()
                .id(UUID.randomUUID())
                .name("Bianca Chen")
                .email("bianca@creators.com")
                .passwordHash("hashed")
                .role(UserRole.CREATOR)
                .active(true)
                .build();

        when(userRepository.findByEmail(creatorA.getEmail())).thenReturn(Optional.of(creatorA));
        when(userRepository.findById(creatorA.getId())).thenReturn(Optional.of(creatorA));
        when(userRepository.findByEmail(creatorB.getEmail())).thenReturn(Optional.of(creatorB));
        when(userRepository.findById(creatorB.getId())).thenReturn(Optional.of(creatorB));

        tokenA = "Bearer " + jwtService.generateAccessToken(creatorA);
        tokenB = "Bearer " + jwtService.generateAccessToken(creatorB);

        recommendationA = ContentRecommendation.builder()
                .id(UUID.randomUUID())
                .user(creatorA)
                .title("Optimizing PostgreSQL Query Plans")
                .angle("Deep dive into indexing")
                .build();

        Map<String, Object> briefContent = new HashMap<>();
        briefContent.put("title", "Optimizing PostgreSQL Query Plans");
        briefContent.put("platform", "YOUTUBE");
        briefContent.put("contentType", "VIDEO");
        briefContent.put("targetAudience", "Database Engineers");
        briefContent.put("audienceProblem", "Slow queries");
        briefContent.put("coreMessage", "Index tuning");
        briefContent.put("hook", "Why are your queries slow?");
        briefContent.put("callToAction", "Subscribe for more tips");

        approvedAsset = ContentProductionAsset.builder()
                .id(UUID.randomUUID())
                .user(creatorA)
                .recommendation(recommendationA)
                .assetType(ProductionAssetType.CONTENT_BRIEF)
                .status(ProductionAssetStatus.APPROVED)
                .generationMode(GenerationMode.EVIDENCE_GROUNDED)
                .version(1)
                .contentJson(briefContent)
                .approvedAt(Instant.now())
                .build();

        unapprovedAsset = ContentProductionAsset.builder()
                .id(UUID.randomUUID())
                .user(creatorA)
                .recommendation(recommendationA)
                .assetType(ProductionAssetType.CONTENT_BRIEF)
                .status(ProductionAssetStatus.GENERATED)
                .generationMode(GenerationMode.EVIDENCE_GROUNDED)
                .version(1)
                .contentJson(briefContent)
                .build();
    }

    @Nested
    @DisplayName("POST /production-assets/{id}/exports")
    class CreateExportTests {

        @Test
        @DisplayName("Should export approved asset to Markdown successfully")
        void testCreateMarkdownExportSuccess() throws Exception {
            when(productionAssetRepository.findByIdAndUserId(approvedAsset.getId(), creatorA.getId()))
                    .thenReturn(Optional.of(approvedAsset));
            when(productionAssetRepository.findAllByUserIdAndRecommendationId(creatorA.getId(), recommendationA.getId()))
                    .thenReturn(List.of(approvedAsset));

            when(exportRepository.save(any(ContentExport.class)))
                    .thenAnswer(inv -> {
                        ContentExport e = inv.getArgument(0);
                        e.setId(UUID.randomUUID());
                        e.setCreatedAt(Instant.now());
                        return e;
                    });

            ExportRequest request = new ExportRequest(ExportType.MARKDOWN);

            mockMvc.perform(post("/production-assets/" + approvedAsset.getId() + "/exports")
                            .header(HttpHeaders.AUTHORIZATION, tokenA)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.success", is(true)))
                    .andExpect(jsonPath("$.data.exportType", is("MARKDOWN")))
                    .andExpect(jsonPath("$.data.fileName", endsWith(".md")))
                    .andExpect(jsonPath("$.data.mimeType", is("text/markdown")))
                    .andExpect(jsonPath("$.data.contentHash", notNullValue()))
                    .andExpect(jsonPath("$.data.downloadUrl", notNullValue()));
        }

        @Test
        @DisplayName("Should export approved asset to PDF successfully")
        void testCreatePdfExportSuccess() throws Exception {
            when(productionAssetRepository.findByIdAndUserId(approvedAsset.getId(), creatorA.getId()))
                    .thenReturn(Optional.of(approvedAsset));
            when(productionAssetRepository.findAllByUserIdAndRecommendationId(creatorA.getId(), recommendationA.getId()))
                    .thenReturn(List.of(approvedAsset));

            when(exportRepository.save(any(ContentExport.class)))
                    .thenAnswer(inv -> {
                        ContentExport e = inv.getArgument(0);
                        e.setId(UUID.randomUUID());
                        e.setCreatedAt(Instant.now());
                        return e;
                    });

            ExportRequest request = new ExportRequest(ExportType.PDF);

            mockMvc.perform(post("/production-assets/" + approvedAsset.getId() + "/exports")
                            .header(HttpHeaders.AUTHORIZATION, tokenA)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.success", is(true)))
                    .andExpect(jsonPath("$.data.exportType", is("PDF")))
                    .andExpect(jsonPath("$.data.mimeType", is("application/pdf")));
        }

        @Test
        @DisplayName("Should return 400 when attempting to export unapproved asset")
        void testExportFailsOnUnapprovedAsset() throws Exception {
            when(productionAssetRepository.findByIdAndUserId(unapprovedAsset.getId(), creatorA.getId()))
                    .thenReturn(Optional.of(unapprovedAsset));

            ExportRequest request = new ExportRequest(ExportType.MARKDOWN);

            mockMvc.perform(post("/production-assets/" + unapprovedAsset.getId() + "/exports")
                            .header(HttpHeaders.AUTHORIZATION, tokenA)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errorCode", is("ASSET_NOT_APPROVED")));
        }

        @Test
        @DisplayName("Should return 404 on cross-user export attempt")
        void testCrossUserExportReturns404() throws Exception {
            when(productionAssetRepository.findByIdAndUserId(approvedAsset.getId(), creatorB.getId()))
                    .thenReturn(Optional.empty());

            ExportRequest request = new ExportRequest(ExportType.MARKDOWN);

            mockMvc.perform(post("/production-assets/" + approvedAsset.getId() + "/exports")
                            .header(HttpHeaders.AUTHORIZATION, tokenB)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.errorCode", is("ASSET_NOT_FOUND")));
        }
    }

    @Nested
    @DisplayName("POST /production-assets/{id}/package")
    class CreatePackageTests {

        @Test
        @DisplayName("Should generate complete ZIP production package")
        void testCreateProductionPackage() throws Exception {
            when(productionAssetRepository.findByIdAndUserId(approvedAsset.getId(), creatorA.getId()))
                    .thenReturn(Optional.of(approvedAsset));
            when(productionAssetRepository.findAllByUserIdAndRecommendationId(creatorA.getId(), recommendationA.getId()))
                    .thenReturn(List.of(approvedAsset));

            when(exportRepository.save(any(ContentExport.class)))
                    .thenAnswer(inv -> {
                        ContentExport e = inv.getArgument(0);
                        e.setId(UUID.randomUUID());
                        e.setCreatedAt(Instant.now());
                        return e;
                    });

            mockMvc.perform(post("/production-assets/" + approvedAsset.getId() + "/package")
                            .header(HttpHeaders.AUTHORIZATION, tokenA))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.success", is(true)))
                    .andExpect(jsonPath("$.data.exportType", is("PRODUCTION_PACKAGE")))
                    .andExpect(jsonPath("$.data.fileName", endsWith("-package.zip")))
                    .andExpect(jsonPath("$.data.mimeType", is("application/zip")));
        }
    }

    @Nested
    @DisplayName("GET /production-exports/{exportId}/download")
    class DownloadExportTests {

        @Test
        @DisplayName("Should download export file with correct headers and SHA-256 integrity hash")
        void testDownloadExportFile() throws Exception {
            UUID exportId = UUID.randomUUID();
            byte[] fileBytes = "# PostgreSQL Tuning Blueprint".getBytes(StandardCharsets.UTF_8);

            ContentExport export = ContentExport.builder()
                    .id(exportId)
                    .user(creatorA)
                    .productionAsset(approvedAsset)
                    .exportType(ExportType.MARKDOWN)
                    .fileName("pulsegpt-production-v1.md")
                    .mimeType("text/markdown")
                    .contentHash("abc123sha256hash")
                    .contentData(fileBytes)
                    .build();

            when(exportRepository.findByIdAndUserId(exportId, creatorA.getId()))
                    .thenReturn(Optional.of(export));

            mockMvc.perform(get("/production-exports/" + exportId + "/download")
                            .header(HttpHeaders.AUTHORIZATION, tokenA))
                    .andExpect(status().isOk())
                    .andExpect(header().string("Content-Type", "text/markdown"))
                    .andExpect(header().string("Content-Disposition", "form-data; name=\"attachment\"; filename=\"pulsegpt-production-v1.md\""))
                    .andExpect(header().string("X-Content-Hash", "abc123sha256hash"))
                    .andExpect(content().bytes(fileBytes));
        }

        @Test
        @DisplayName("Should return 404 if export belongs to another user")
        void testDownloadCrossUserReturns404() throws Exception {
            UUID exportId = UUID.randomUUID();
            when(exportRepository.findByIdAndUserId(exportId, creatorB.getId()))
                    .thenReturn(Optional.empty());

            mockMvc.perform(get("/production-exports/" + exportId + "/download")
                            .header(HttpHeaders.AUTHORIZATION, tokenB))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.errorCode", is("EXPORT_NOT_FOUND")));
        }
    }

    @Nested
    @DisplayName("GET /production-assets/{id}/exports")
    class ListExportsTests {

        @Test
        @DisplayName("Should list previous exports for asset")
        void testListExportsForAsset() throws Exception {
            when(productionAssetRepository.findByIdAndUserId(approvedAsset.getId(), creatorA.getId()))
                    .thenReturn(Optional.of(approvedAsset));

            ContentExport export = ContentExport.builder()
                    .id(UUID.randomUUID())
                    .user(creatorA)
                    .productionAsset(approvedAsset)
                    .exportType(ExportType.MARKDOWN)
                    .fileName("export.md")
                    .mimeType("text/markdown")
                    .contentHash("hash123")
                    .version(1)
                    .createdAt(Instant.now())
                    .build();

            when(exportRepository.findAllByUserIdAndProductionAssetId(eq(creatorA.getId()), eq(approvedAsset.getId()), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of(export)));

            mockMvc.perform(get("/production-assets/" + approvedAsset.getId() + "/exports")
                            .header(HttpHeaders.AUTHORIZATION, tokenA))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success", is(true)))
                    .andExpect(jsonPath("$.data.content", hasSize(1)))
                    .andExpect(jsonPath("$.data.content[0].exportType", is("MARKDOWN")));
        }
    }
}
