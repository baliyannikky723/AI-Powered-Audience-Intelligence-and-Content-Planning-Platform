package com.pulsegpt;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pulsegpt.export.ExportType;
import com.pulsegpt.export.dto.TimelineSceneDto;
import com.pulsegpt.export.service.ContentExportService;
import com.pulsegpt.export.service.ExportFormattingService;
import com.pulsegpt.production.ContentProductionAsset;
import com.pulsegpt.production.ProductionAssetStatus;
import com.pulsegpt.production.ProductionAssetType;
import com.pulsegpt.recommendation.ContentRecommendation;
import com.pulsegpt.recommendation.GenerationMode;
import com.pulsegpt.user.User;
import com.pulsegpt.user.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Phase 3L — ExportFormattingService Unit Tests")
class ExportFormattingServiceTest {

    private ExportFormattingService formattingService;
    private ObjectMapper objectMapper;

    private User testUser;
    private ContentRecommendation testRecommendation;
    private ContentProductionAsset briefAsset;
    private List<ContentProductionAsset> allAssets;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        formattingService = new ExportFormattingService(objectMapper);

        testUser = User.builder()
                .id(UUID.randomUUID())
                .name("Alex Rivera")
                .email("alex@creators.com")
                .role(UserRole.CREATOR)
                .build();

        testRecommendation = ContentRecommendation.builder()
                .id(UUID.randomUUID())
                .user(testUser)
                .title("Optimizing PostgreSQL Query Plans")
                .angle("Deep dive into indexing and query optimization")
                .build();

        Map<String, Object> briefContent = new HashMap<>();
        briefContent.put("title", "Optimizing PostgreSQL Query Plans");
        briefContent.put("platform", "YouTube");
        briefContent.put("contentType", "VIDEO");
        briefContent.put("targetAudience", "Database Engineers");
        briefContent.put("audienceProblem", "Slow unindexed queries in production");
        briefContent.put("contentAngle", "Real-world query plan tuning");
        briefContent.put("coreMessage", "Systematic index optimization cuts query latency by 90%");
        briefContent.put("tone", "Technical & Practical");
        briefContent.put("hook", "Why does your PostgreSQL query take 5 seconds?");
        briefContent.put("callToAction", "Subscribe for weekly database performance breakdowns.");
        briefContent.put("successObjective", "Help engineers write fast SQL");
        briefContent.put("audienceEvidence", List.of(
                "Why is my join query taking so long?",
                "How do I use EXPLAIN ANALYZE effectively?"
        ));

        briefAsset = ContentProductionAsset.builder()
                .id(UUID.randomUUID())
                .user(testUser)
                .recommendation(testRecommendation)
                .assetType(ProductionAssetType.CONTENT_BRIEF)
                .status(ProductionAssetStatus.APPROVED)
                .generationMode(GenerationMode.EVIDENCE_GROUNDED)
                .version(1)
                .promptVersion("PRODUCTION_PROMPT_V1")
                .modelName("pulsegpt-production-v1")
                .contentJson(briefContent)
                .evidenceSnapshot(Map.of("evidenceQuotes", List.of("Why is my join query taking so long?")))
                .createdAt(Instant.now())
                .approvedAt(Instant.now())
                .build();

        Map<String, Object> outlineContent = Map.of(
                "format", "VIDEO_10_PART_STRUCTURE",
                "keyTakeaway", "Indexes and vacuuming are essential for sustained PostgreSQL performance.",
                "sections", List.of(
                        Map.of("sectionTitle", "1. Hook", "purpose", "Engage viewers", "talkingPoints", List.of("Show slow vs fast query execution")),
                        Map.of("sectionTitle", "2. Understanding EXPLAIN ANALYZE", "purpose", "Explain cost metrics", "talkingPoints", List.of("Break down Seq Scan vs Index Scan")),
                        Map.of("sectionTitle", "3. B-Tree vs GIN Indexes", "purpose", "Index selection", "talkingPoints", List.of("When to choose B-Tree", "When to use GIN for JSONB"))
                )
        );

        ContentProductionAsset outlineAsset = ContentProductionAsset.builder()
                .id(UUID.randomUUID())
                .user(testUser)
                .recommendation(testRecommendation)
                .assetType(ProductionAssetType.SCRIPT_OUTLINE)
                .status(ProductionAssetStatus.APPROVED)
                .contentJson(outlineContent)
                .build();

        Map<String, Object> hooksContent = Map.of("hooks", List.of(
                Map.of("hookType", "QUESTION", "text", "Why is your PostgreSQL query taking seconds instead of milliseconds?", "rationale", "Direct curiosity question"),
                Map.of("hookType", "PROBLEM", "text", "Your database CPU is at 100% because of this one missing index.", "rationale", "High urgency problem hook")
        ));

        ContentProductionAsset hooksAsset = ContentProductionAsset.builder()
                .id(UUID.randomUUID())
                .user(testUser)
                .recommendation(testRecommendation)
                .assetType(ProductionAssetType.HOOK)
                .status(ProductionAssetStatus.APPROVED)
                .contentJson(hooksContent)
                .build();

        Map<String, Object> titlesContent = Map.of("titles", List.of(
                Map.of("titleType", "CURIOSITY", "text", "The 1 PostgreSQL Index You Should Never Forget", "rationale", "Curiosity"),
                Map.of("titleType", "HOW_TO", "text", "How to Read PostgreSQL EXPLAIN ANALYZE Like a Senior DBA", "rationale", "Actionable guide")
        ));

        ContentProductionAsset titlesAsset = ContentProductionAsset.builder()
                .id(UUID.randomUUID())
                .user(testUser)
                .recommendation(testRecommendation)
                .assetType(ProductionAssetType.TITLE_VARIATION)
                .status(ProductionAssetStatus.APPROVED)
                .contentJson(titlesContent)
                .build();

        Map<String, Object> ctasContent = Map.of("ctas", List.of(
                Map.of("ctaType", "COMMENT", "text", "What is the slowest query running in your production database right now? Let me know below.")
        ));

        ContentProductionAsset ctasAsset = ContentProductionAsset.builder()
                .id(UUID.randomUUID())
                .user(testUser)
                .recommendation(testRecommendation)
                .assetType(ProductionAssetType.CTA)
                .status(ProductionAssetStatus.APPROVED)
                .contentJson(ctasContent)
                .build();

        Map<String, Object> thumbContent = Map.of(
                "visualSubject", "Split screen showing red slow query execution vs green fast query",
                "textOverlay", "100x FASTER SQL",
                "composition", "High contrast dark mode diagram with neon accents",
                "concept", "Visual contrast between unindexed and indexed scan times"
        );

        ContentProductionAsset thumbAsset = ContentProductionAsset.builder()
                .id(UUID.randomUUID())
                .user(testUser)
                .recommendation(testRecommendation)
                .assetType(ProductionAssetType.THUMBNAIL_PROMPT)
                .status(ProductionAssetStatus.APPROVED)
                .contentJson(thumbContent)
                .build();

        Map<String, Object> checklistContent = Map.of("items", List.of(
                Map.of("phase", "PRE_PRODUCTION", "task", "Prepare test PostgreSQL schema and benchmarks", "completed", true),
                Map.of("phase", "PRODUCTION", "task", "Record screen capture of EXPLAIN ANALYZE", "completed", false),
                Map.of("phase", "POST_PRODUCTION", "task", "Add zoom-ins on query plan trees", "completed", false)
        ));

        ContentProductionAsset checklistAsset = ContentProductionAsset.builder()
                .id(UUID.randomUUID())
                .user(testUser)
                .recommendation(testRecommendation)
                .assetType(ProductionAssetType.PRODUCTION_CHECKLIST)
                .status(ProductionAssetStatus.APPROVED)
                .contentJson(checklistContent)
                .build();

        allAssets = List.of(briefAsset, outlineAsset, hooksAsset, titlesAsset, ctasAsset, thumbAsset, checklistAsset);
    }

    @Test
    @DisplayName("Generate Markdown Export contains all structured sections and evidence provenance")
    void testGenerateMarkdownCorrectness() {
        String md = formattingService.generateMarkdown(briefAsset, allAssets);

        assertThat(md).contains("# Optimizing PostgreSQL Query Plans");
        assertThat(md).contains("## 1. Content Brief");
        assertThat(md).contains("## 2. Recommended Title Options");
        assertThat(md).contains("The 1 PostgreSQL Index You Should Never Forget");
        assertThat(md).contains("## 3. Hook Variations");
        assertThat(md).contains("Why is your PostgreSQL query taking seconds");
        assertThat(md).contains("## 4. Script & Content Outline");
        assertThat(md).contains("Understanding EXPLAIN ANALYZE");
        assertThat(md).contains("## 5. Call-to-Action Variations");
        assertThat(md).contains("What is the slowest query");
        assertThat(md).contains("## 6. Thumbnail Concept & Visual Direction");
        assertThat(md).contains("100x FASTER SQL");
        assertThat(md).contains("## 7. Production Checklist");
        assertThat(md).contains("[x] **PRE_PRODUCTION**: Prepare test PostgreSQL schema");
        assertThat(md).contains("## 8. Audience Evidence & Provenance Appendix");
        assertThat(md).contains(testRecommendation.getId().toString());
    }

    @Test
    @DisplayName("Generate Teleprompter Export produces clean readable script without metadata clutter")
    void testGenerateTeleprompterFormatting() {
        String script = formattingService.generateTeleprompter(briefAsset, allAssets);

        assertThat(script).contains("[HOOK]");
        assertThat(script).contains("Why is your PostgreSQL query taking seconds");
        assertThat(script).contains("[INTRODUCTION & THE CORE PROBLEM]");
        assertThat(script).contains("Slow unindexed queries in production");
        assertThat(script).contains("[2. UNDERSTANDING EXPLAIN ANALYZE]");
        assertThat(script).contains("[SUMMARY & KEY TAKEAWAY]");
        assertThat(script).contains("[CALL TO ACTION]");
        assertThat(script).contains("[END OF SCRIPT]");
        assertThat(script).doesNotContain("metadataJson");
        assertThat(script).doesNotContain("evidenceSnapshot");
    }

    @Test
    @DisplayName("Generate Timeline Breakdown calculates pacing and estimated scene durations")
    void testGenerateTimelineCalculationAndPacing() {
        List<TimelineSceneDto> scenes = formattingService.generateTimeline(briefAsset, allAssets);

        assertThat(scenes).isNotEmpty();
        assertThat(scenes.get(0).sectionTitle()).contains("Hook");
        assertThat(scenes.get(0).estimatedDurationSeconds()).isEqualTo(10);

        String timelineMd = formattingService.generateTimelineMarkdown(briefAsset, allAssets);
        assertThat(timelineMd).contains("# Production Timeline & Scene Breakdown");
        assertThat(timelineMd).contains("Estimated Total Duration");
        assertThat(timelineMd).contains("| Scene | Section | Purpose | Est. Window |");
    }

    @Test
    @DisplayName("Generate Checklist Export formats Markdown with phase groupings and checkboxes")
    void testGenerateChecklistCompleteness() {
        String checklistMd = formattingService.generateChecklistMarkdown(briefAsset, allAssets);

        assertThat(checklistMd).contains("# Production Workflow Checklist");
        assertThat(checklistMd).contains("PRE PRODUCTION");
        assertThat(checklistMd).contains("- [x] Prepare test PostgreSQL schema");
        assertThat(checklistMd).contains("- [ ] Record screen capture of EXPLAIN ANALYZE");
    }

    @Test
    @DisplayName("Generate JSON Export produces valid parseable JSON with complete provenance")
    void testGenerateJsonSchemaAndProvenance() throws Exception {
        String json = formattingService.generateJson(briefAsset, allAssets);

        assertThat(json).isNotBlank();
        Map<?, ?> parsed = objectMapper.readValue(json, Map.class);
        assertThat(parsed.get("project")).isEqualTo("PulseGPT Audience Intelligence Platform");
        assertThat(parsed.get("productionAssetId")).isEqualTo(briefAsset.getId().toString());
        assertThat(parsed.get("recommendationId")).isEqualTo(testRecommendation.getId().toString());
        assertThat(parsed.get("contentBrief")).isNotNull();
        assertThat(parsed.get("titles")).isNotNull();
        assertThat(parsed.get("hooks")).isNotNull();
        assertThat(parsed.get("timeline")).isNotNull();
    }

    @Test
    @DisplayName("Generate PDF Export outputs valid non-empty PDF byte stream")
    void testGeneratePdfDeterministicBytes() {
        byte[] pdfBytes = formattingService.generatePdf(briefAsset, allAssets);

        assertThat(pdfBytes).isNotEmpty();
        // PDF header magic bytes: %PDF
        String header = new String(pdfBytes, 0, Math.min(pdfBytes.length, 5), StandardCharsets.US_ASCII);
        assertThat(header).startsWith("%PDF");
    }

    @Test
    @DisplayName("Generate Production Package ZIP contains all 13 required files including PDF, scripts, and README")
    void testGenerateProductionPackageZipStructure() throws Exception {
        byte[] zipBytes = formattingService.generateProductionPackageZip(briefAsset, allAssets);

        assertThat(zipBytes).isNotEmpty();

        Set<String> entryNames = new HashSet<>();
        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(zipBytes))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                entryNames.add(entry.getName());
                zis.closeEntry();
            }
        }

        assertThat(entryNames).contains(
                "content-brief.md",
                "script-outline.md",
                "teleprompter.txt",
                "titles.md",
                "hooks.md",
                "ctas.md",
                "thumbnail-prompt.md",
                "checklist.md",
                "timeline.md",
                "evidence.json",
                "content.json",
                "production-pack.pdf",
                "README.md"
        );
    }

    @Test
    @DisplayName("Content Hash is deterministic and correctly computed with SHA-256")
    void testContentHashIntegrity() {
        byte[] data1 = "PulseGPT Deterministic Export Data".getBytes(StandardCharsets.UTF_8);
        byte[] data2 = "PulseGPT Deterministic Export Data".getBytes(StandardCharsets.UTF_8);
        byte[] data3 = "Different Data".getBytes(StandardCharsets.UTF_8);

        String hash1 = ContentExportService.computeSha256(data1);
        String hash2 = ContentExportService.computeSha256(data2);
        String hash3 = ContentExportService.computeSha256(data3);

        assertThat(hash1).isEqualTo(hash2);
        assertThat(hash1).hasSize(64); // 256 bits = 64 hex chars
        assertThat(hash1).isNotEqualTo(hash3);
    }
}
