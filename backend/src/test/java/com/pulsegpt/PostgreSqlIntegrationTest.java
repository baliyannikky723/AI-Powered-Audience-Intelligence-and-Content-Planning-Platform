package com.pulsegpt;

import com.pulsegpt.comment.*;
import com.pulsegpt.platform.*;
import com.pulsegpt.topic.*;
import com.pulsegpt.user.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DisplayName("PostgreSQL + pgvector Container Integration Tests")
class PostgreSqlIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(
            DockerImageName.parse("pgvector/pgvector:pg16")
    )
            .withDatabaseName("pulsegpt_test")
            .withUsername("pulsegpt")
            .withPassword("pulsegpt_test_secret");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.flyway.enabled", () -> "true");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
    }

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PlatformAccountRepository platformAccountRepository;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private RawCommentRepository rawCommentRepository;

    @Autowired
    private ProcessedCommentRepository processedCommentRepository;

    @Autowired
    private TopicRepository topicRepository;

    @Autowired
    private TopicMetricsDailyRepository topicMetricsDailyRepository;

    @Test
    @DisplayName("Verify full database lifecycle: Users, PlatformAccounts, Posts, Comments, Topics, JSONB & pgvector")
    void testDatabaseLifecycle() {
        // 1. Create User
        User user = User.builder()
                .name("Test Creator")
                .email("creator@pulsegpt.ai")
                .passwordHash("$2a$12$e8m4y...hash")
                .role(UserRole.CREATOR)
                .active(true)
                .build();
        User savedUser = userRepository.save(user);
        assertThat(savedUser.getId()).isNotNull();
        assertThat(savedUser.getEmail()).isEqualTo("creator@pulsegpt.ai");

        // 2. Create Platform Account
        PlatformAccount account = PlatformAccount.builder()
                .user(savedUser)
                .platform(PlatformType.YOUTUBE)
                .externalAccountId("UC_test_12345")
                .accountName("Pulse Channel")
                .metadata(Map.of("channel_id", "UC_test_12345", "subscribers", 15000))
                .status(PlatformAccountStatus.CONNECTED)
                .build();
        PlatformAccount savedAccount = platformAccountRepository.save(account);
        assertThat(savedAccount.getId()).isNotNull();
        assertThat(savedAccount.getMetadata()).containsEntry("subscribers", 15000);

        // 3. Create Post
        Post post = Post.builder()
                .platformAccount(savedAccount)
                .externalPostId("video_xyz789")
                .title("Top AI Tools for Content Creators")
                .url("https://youtube.com/watch?v=xyz789")
                .publishedAt(Instant.now())
                .viewsCount(12000L)
                .likesCount(850L)
                .commentsCount(120L)
                .metadata(Map.of("duration", 640))
                .build();
        Post savedPost = postRepository.save(post);
        assertThat(savedPost.getId()).isNotNull();

        // 4. Create Raw Comment
        RawComment rawComment = RawComment.builder()
                .post(savedPost)
                .externalCommentId("comm_111")
                .authorDisplayName("DevFan")
                .rawText("This tutorial helped me launch my channel!")
                .publishedAt(Instant.now())
                .likes(12)
                .build();
        RawComment savedRawComment = rawCommentRepository.save(rawComment);
        assertThat(savedRawComment.getId()).isNotNull();

        // 5. Create Processed Comment with Embedding
        ProcessedComment processedComment = ProcessedComment.builder()
                .rawComment(savedRawComment)
                .normalizedText("this tutorial helped me launch my channel")
                .language("en")
                .sentimentLabel(SentimentLabel.POSITIVE)
                .sentimentScore(0.92)
                .intent(IntentType.PRAISE)
                .priority(Priority.HIGH)
                .embedding("[0.123, -0.456, 0.789]")
                .embeddingModel("text-embedding-3-small")
                .embeddingDimension(1536)
                .processingVersion("v1.0")
                .build();
        ProcessedComment savedProcessedComment = processedCommentRepository.save(processedComment);
        assertThat(savedProcessedComment.getId()).isNotNull();
        assertThat(savedProcessedComment.getSentimentLabel()).isEqualTo(SentimentLabel.POSITIVE);

        // 6. Create Topic
        Topic topic = Topic.builder()
                .user(savedUser)
                .name("AI Content Tools")
                .description("Discussion on automated video workflows")
                .centroid("[0.111, -0.222, 0.333]")
                .algorithm("kmeans")
                .algorithmVersion("1.0")
                .build();
        Topic savedTopic = topicRepository.save(topic);
        assertThat(savedTopic.getId()).isNotNull();

        // 7. Create Topic Metrics Daily
        TopicMetricsDaily metric = TopicMetricsDaily.builder()
                .topic(savedTopic)
                .metricDate(LocalDate.now())
                .commentCount(45)
                .uniqueCommentCount(38)
                .engagement(150L)
                .averageSentiment(0.85)
                .build();
        TopicMetricsDaily savedMetric = topicMetricsDailyRepository.save(metric);
        assertThat(savedMetric.getId()).isNotNull();
        assertThat(savedMetric.getCommentCount()).isEqualTo(45);
    }
}
