package com.pulsegpt;

import com.pulsegpt.admin.ApiQuotaUsage;
import com.pulsegpt.audit.AuditLog;
import com.pulsegpt.auth.RefreshToken;
import com.pulsegpt.calendar.CalendarItem;
import com.pulsegpt.calendar.CalendarItemStatus;
import com.pulsegpt.chat.ChatMessage;
import com.pulsegpt.chat.ChatMessageRole;
import com.pulsegpt.chat.ChatSession;
import com.pulsegpt.comment.*;
import com.pulsegpt.evaluation.Annotation;
import com.pulsegpt.evaluation.AnnotationLabelType;
import com.pulsegpt.evaluation.ExperimentRun;
import com.pulsegpt.ingestion.IngestionRun;
import com.pulsegpt.ingestion.IngestionRunStatus;
import com.pulsegpt.ingestion.IngestionState;
import com.pulsegpt.memory.AudienceInterest;
import com.pulsegpt.memory.AudienceInterestStatus;
import com.pulsegpt.notification.Notification;
import com.pulsegpt.notification.NotificationType;
import com.pulsegpt.platform.PlatformAccount;
import com.pulsegpt.platform.PlatformAccountStatus;
import com.pulsegpt.platform.PlatformType;
import com.pulsegpt.recommendation.ContentRecommendation;
import com.pulsegpt.recommendation.RecommendationMode;
import com.pulsegpt.topic.*;
import com.pulsegpt.trend.TrendScore;
import com.pulsegpt.trend.TrendStatus;
import com.pulsegpt.user.User;
import com.pulsegpt.user.UserRole;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Entity and Domain Model Unit Tests")
class EntityDomainModelTest {

    @Test
    @DisplayName("Verify all 21 Phase 3B entities and builder/instantiation patterns")
    void testAllEntitiesInstantiation() {
        UUID testId = UUID.randomUUID();

        // 1. User
        User user = User.builder()
                .id(testId)
                .name("Creator User")
                .email("creator@example.com")
                .passwordHash("hashed-password")
                .role(UserRole.CREATOR)
                .active(true)
                .build();
        assertThat(user.getRole()).isEqualTo(UserRole.CREATOR);

        // 2. RefreshToken
        RefreshToken refreshToken = RefreshToken.builder()
                .id(UUID.randomUUID())
                .user(user)
                .tokenHash("token-hash-123")
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();
        assertThat(refreshToken.getTokenHash()).isEqualTo("token-hash-123");

        // 3. PlatformAccount
        PlatformAccount platformAccount = PlatformAccount.builder()
                .id(UUID.randomUUID())
                .user(user)
                .platform(PlatformType.YOUTUBE)
                .externalAccountId("UC123456")
                .accountName("YouTube Main")
                .status(PlatformAccountStatus.CONNECTED)
                .metadata(Map.of("category", "tech"))
                .build();
        assertThat(platformAccount.getPlatform()).isEqualTo(PlatformType.YOUTUBE);

        // 4. Post
        Post post = Post.builder()
                .id(UUID.randomUUID())
                .platformAccount(platformAccount)
                .externalPostId("post_123")
                .title("PulseGPT Walkthrough")
                .publishedAt(Instant.now())
                .viewsCount(5000L)
                .build();
        assertThat(post.getTitle()).isEqualTo("PulseGPT Walkthrough");

        // 5. IngestionRun
        IngestionRun ingestionRun = IngestionRun.builder()
                .id(UUID.randomUUID())
                .platformAccount(platformAccount)
                .status(IngestionRunStatus.COMPLETED)
                .fetchedCount(100)
                .insertedCount(95)
                .duplicateCount(5)
                .build();
        assertThat(ingestionRun.getFetchedCount()).isEqualTo(100);

        // 6. IngestionState
        IngestionState ingestionState = IngestionState.builder()
                .id(UUID.randomUUID())
                .platformAccount(platformAccount)
                .cursorToken("cursor_token_xyz")
                .failureCount(0)
                .build();
        assertThat(ingestionState.getCursorToken()).isEqualTo("cursor_token_xyz");

        // 7. RawComment
        RawComment rawComment = RawComment.builder()
                .id(UUID.randomUUID())
                .post(post)
                .externalCommentId("c_123")
                .rawText("Awesome video!")
                .publishedAt(Instant.now())
                .likes(5)
                .build();
        assertThat(rawComment.getRawText()).isEqualTo("Awesome video!");

        // 8. ProcessedComment
        ProcessedComment processedComment = ProcessedComment.builder()
                .id(UUID.randomUUID())
                .rawComment(rawComment)
                .normalizedText("awesome video")
                .language("en")
                .sentimentLabel(SentimentLabel.POSITIVE)
                .sentimentScore(0.95)
                .intent(IntentType.PRAISE)
                .priority(Priority.HIGH)
                .embedding("[0.1, 0.2, 0.3]")
                .embeddingModel("text-embedding-3-small")
                .embeddingDimension(1536)
                .processingVersion("v1.0")
                .build();
        assertThat(processedComment.getSentimentScore()).isEqualTo(0.95);

        // 9. Topic
        Topic topic = Topic.builder()
                .id(UUID.randomUUID())
                .user(user)
                .name("Spring Boot Best Practices")
                .description("Discussion around Spring Boot 3 architecture")
                .keywords(List.of("spring", "java", "architecture"))
                .centroid("[0.5, 0.5, 0.5]")
                .algorithm("kmeans")
                .algorithmVersion("1.0")
                .active(true)
                .build();
        assertThat(topic.getKeywords()).contains("java");

        // 10. TopicMetricsDaily
        TopicMetricsDaily topicMetricsDaily = TopicMetricsDaily.builder()
                .id(UUID.randomUUID())
                .topic(topic)
                .metricDate(LocalDate.now())
                .commentCount(20)
                .engagement(50L)
                .build();
        assertThat(topicMetricsDaily.getCommentCount()).isEqualTo(20);

        // 11. TrendScore
        TrendScore trendScore = TrendScore.builder()
                .id(UUID.randomUUID())
                .topic(topic)
                .metricDate(LocalDate.now())
                .frequencyScore(0.8)
                .totalScore(0.85)
                .status(TrendStatus.EMERGING)
                .algorithmVersion("1.0")
                .build();
        assertThat(trendScore.getStatus()).isEqualTo(TrendStatus.EMERGING);

        // 12. AudienceInterest
        AudienceInterest audienceInterest = AudienceInterest.builder()
                .id(UUID.randomUUID())
                .user(user)
                .topic(topic)
                .confidence(0.92)
                .evidenceCount(15)
                .status(AudienceInterestStatus.ACTIVE)
                .build();
        assertThat(audienceInterest.getStatus()).isEqualTo(AudienceInterestStatus.ACTIVE);

        // 13. ContentRecommendation
        ContentRecommendation recommendation = ContentRecommendation.builder()
                .id(UUID.randomUUID())
                .user(user)
                .topic(topic)
                .title("5 Spring Boot Mistakes to Avoid")
                .priority(Priority.HIGH)
                .mode(RecommendationMode.PROPOSED)
                .keyPoints(List.of("Avoid blocking calls", "Use Flyway"))
                .build();
        assertThat(recommendation.getMode()).isEqualTo(RecommendationMode.PROPOSED);

        // 14. CalendarItem
        CalendarItem calendarItem = CalendarItem.builder()
                .id(UUID.randomUUID())
                .user(user)
                .recommendation(recommendation)
                .topic(topic)
                .title("Publish 5 Spring Boot Mistakes")
                .platform(PlatformType.YOUTUBE)
                .status(CalendarItemStatus.SCHEDULED)
                .scheduledStart(Instant.now().plusSeconds(86400))
                .scheduledEnd(Instant.now().plusSeconds(90000))
                .scheduledAt(Instant.now().plusSeconds(86400))
                .timezone("Asia/Kolkata")
                .priority(Priority.HIGH)
                .notes("Production tutorial notes")
                .approvedAt(Instant.now())
                .build();
        assertThat(calendarItem.getStatus()).isEqualTo(CalendarItemStatus.SCHEDULED);
        assertThat(calendarItem.getTimezone()).isEqualTo("Asia/Kolkata");
        assertThat(calendarItem.getPriority()).isEqualTo(Priority.HIGH);

        // 15. ChatSession
        ChatSession chatSession = ChatSession.builder()
                .id(UUID.randomUUID())
                .user(user)
                .title("Audience Strategy Q&A")
                .build();
        assertThat(chatSession.getTitle()).isEqualTo("Audience Strategy Q&A");

        // 16. ChatMessage
        ChatMessage chatMessage = ChatMessage.builder()
                .id(UUID.randomUUID())
                .session(chatSession)
                .role(ChatMessageRole.USER)
                .content("What topics are trending this week?")
                .build();
        assertThat(chatMessage.getRole()).isEqualTo(ChatMessageRole.USER);

        // 17. Notification
        Notification notification = Notification.builder()
                .id(UUID.randomUUID())
                .user(user)
                .type(NotificationType.INSIGHT)
                .title("New Recommendation")
                .message("We found 3 high-affinity topics for your next video.")
                .build();
        assertThat(notification.getType()).isEqualTo(NotificationType.INSIGHT);

        // 18. AuditLog
        AuditLog auditLog = AuditLog.builder()
                .id(UUID.randomUUID())
                .user(user)
                .action("ACCOUNT_CONNECT")
                .resourceType("PLATFORM_ACCOUNT")
                .resourceId(platformAccount.getId().toString())
                .correlationId("corr-12345")
                .build();
        assertThat(auditLog.getAction()).isEqualTo("ACCOUNT_CONNECT");

        // 19. ApiQuotaUsage
        ApiQuotaUsage quotaUsage = ApiQuotaUsage.builder()
                .id(UUID.randomUUID())
                .platformAccount(platformAccount)
                .usageDate(LocalDate.now())
                .quotaUsed(1200L)
                .quotaLimit(10000L)
                .warningThreshold(80)
                .build();
        assertThat(quotaUsage.getQuotaUsed()).isEqualTo(1200L);

        // 20. ExperimentRun
        ExperimentRun experimentRun = ExperimentRun.builder()
                .id(UUID.randomUUID())
                .user(user)
                .experimentName("Clustering Comparison")
                .experimentType("TOPIC_CLUSTERING")
                .algorithmVersion("2.1")
                .status("COMPLETED")
                .build();
        assertThat(experimentRun.getExperimentType()).isEqualTo("TOPIC_CLUSTERING");

        // 21. Annotation
        Annotation annotation = Annotation.builder()
                .id(UUID.randomUUID())
                .processedComment(processedComment)
                .annotator(user)
                .labelType(AnnotationLabelType.SENTIMENT)
                .label("POSITIVE")
                .confidence(0.99)
                .build();
        assertThat(annotation.getLabelType()).isEqualTo(AnnotationLabelType.SENTIMENT);

        // 22. Phase 3H: ClusteringRun
        ClusteringRun clusteringRun = ClusteringRun.builder()
                .id(UUID.randomUUID())
                .user(user)
                .status(ClusteringRunStatus.COMPLETED)
                .algorithm("SEMANTIC_UMAP_HDBSCAN_CTFIDF")
                .algorithmVersion("pulsegpt-cluster-v1")
                .embeddingModel("sentence-transformers/all-MiniLM-L6-v2")
                .embeddingDimension(384)
                .clusterCount(3)
                .clusteredCommentCount(25)
                .noiseCount(4)
                .config(Map.of("minClusterSize", 3))
                .metrics(Map.of("silhouetteScore", 0.65))
                .build();
        assertThat(clusteringRun.getStatus()).isEqualTo(ClusteringRunStatus.COMPLETED);
        assertThat(clusteringRun.getClusterCount()).isEqualTo(3);

        // 23. Phase 3H: TopicAssignment
        TopicAssignment topicAssignment = TopicAssignment.builder()
                .id(UUID.randomUUID())
                .topic(topic)
                .processedComment(processedComment)
                .clusteringRun(clusteringRun)
                .clusterId(0)
                .isNoise(false)
                .membershipProbability(0.95)
                .build();
        assertThat(topicAssignment.getClusterId()).isEqualTo(0);
        assertThat(topicAssignment.isNoise()).isFalse();

        // 24. Phase 3K: ContentProductionAsset
        com.pulsegpt.production.ContentProductionAsset prodAsset = com.pulsegpt.production.ContentProductionAsset.builder()
                .id(UUID.randomUUID())
                .user(user)
                .recommendation(recommendation)
                .calendarItem(calendarItem)
                .assetType(com.pulsegpt.production.ProductionAssetType.CONTENT_BRIEF)
                .status(com.pulsegpt.production.ProductionAssetStatus.APPROVED)
                .generationMode(com.pulsegpt.recommendation.GenerationMode.EVIDENCE_GROUNDED)
                .version(1)
                .contentJson(Map.of("title", "Mastering Spring Boot"))
                .build();
        assertThat(prodAsset.getAssetType()).isEqualTo(com.pulsegpt.production.ProductionAssetType.CONTENT_BRIEF);
        assertThat(prodAsset.getStatus()).isEqualTo(com.pulsegpt.production.ProductionAssetStatus.APPROVED);
        assertThat(prodAsset.getVersion()).isEqualTo(1);

        // 25. Phase 3L: ContentExport
        com.pulsegpt.export.ContentExport contentExport = com.pulsegpt.export.ContentExport.builder()
                .id(UUID.randomUUID())
                .user(user)
                .productionAsset(prodAsset)
                .exportType(com.pulsegpt.export.ExportType.MARKDOWN)
                .fileName("pulsegpt-production-v1.md")
                .mimeType("text/markdown")
                .contentHash("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855")
                .version(1)
                .fileSizeBytes(1024L)
                .metadataJson(Map.of("exportTimestamp", Instant.now().toString()))
                .build();
        assertThat(contentExport.getExportType()).isEqualTo(com.pulsegpt.export.ExportType.MARKDOWN);
        assertThat(contentExport.getContentHash()).isNotBlank();
        assertThat(contentExport.getVersion()).isEqualTo(1);

        // 26. Phase 3M: DatasetSnapshot
        com.pulsegpt.evaluation.DatasetSnapshot snapshot = com.pulsegpt.evaluation.DatasetSnapshot.builder()
                .id(UUID.randomUUID())
                .user(user)
                .name("Q4 Research Freeze")
                .platform("YOUTUBE")
                .commentCount(100)
                .processedCommentCount(95)
                .embeddingModel("sentence-transformers/all-MiniLM-L6-v2")
                .build();
        assertThat(snapshot.getName()).isEqualTo("Q4 Research Freeze");
        assertThat(snapshot.getCommentCount()).isEqualTo(100);

        // 27. Phase 3M: EvaluationRecord
        com.pulsegpt.evaluation.EvaluationRecord evalRecord = com.pulsegpt.evaluation.EvaluationRecord.builder()
                .id(UUID.randomUUID())
                .user(user)
                .targetType("RECOMMENDATION")
                .targetId(UUID.randomUUID())
                .generationMode("EVIDENCE_GROUNDED")
                .metrics(Map.of("evidenceCoverage", 1.0))
                .build();
        assertThat(evalRecord.getTargetType()).isEqualTo("RECOMMENDATION");
        assertThat(evalRecord.getGenerationMode()).isEqualTo("EVIDENCE_GROUNDED");
    }
}


