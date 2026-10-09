package com.pulsegpt;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Flyway Migration Scripts Integrity Test")
class FlywayMigrationsTest {

    @Test
    @DisplayName("Verify V1, V2, V3 Flyway migration files exist and contain required schema definitions")
    void testFlywayScriptsIntegrity() throws Exception {
        // V1 Migration (Extensions)
        ClassPathResource v1 = new ClassPathResource("db/migration/V1__initial_schema.sql");
        assertThat(v1.exists()).isTrue();
        try (InputStream is = v1.getInputStream()) {
            String content = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            assertThat(content).contains("uuid-ossp");
            assertThat(content).contains("vector");
        }

        // V2 Migration (21 Core Tables)
        ClassPathResource v2 = new ClassPathResource("db/migration/V2__core_tables.sql");
        assertThat(v2.exists()).isTrue();
        try (InputStream is = v2.getInputStream()) {
            String content = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            String[] requiredTables = {
                    "users", "refresh_tokens", "platform_accounts", "posts",
                    "ingestion_runs", "ingestion_state", "raw_comments", "processed_comments",
                    "topics", "topic_metrics_daily", "trend_scores", "audience_interests",
                    "content_recommendations", "calendar_items", "chat_sessions", "chat_messages",
                    "notifications", "audit_logs", "api_quota_usage", "experiment_runs", "annotations"
            };
            for (String table : requiredTables) {
                assertThat(content).as("Check table creation for " + table).contains("CREATE TABLE IF NOT EXISTS " + table);
            }
        }

        // V3 Migration (Indexes and Constraints)
        ClassPathResource v3 = new ClassPathResource("db/migration/V3__indexes_and_constraints.sql");
        assertThat(v3.exists()).isTrue();
        try (InputStream is = v3.getInputStream()) {
            String content = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            assertThat(content).contains("idx_users_email");
            assertThat(content).contains("idx_refresh_tokens_token_hash");
            assertThat(content).contains("idx_platform_accounts_user_id");
            assertThat(content).contains("idx_posts_platform_account");
            assertThat(content).contains("idx_raw_comments_post_id");
            assertThat(content).contains("idx_processed_comments_language");
            assertThat(content).contains("idx_topics_user_id");
            assertThat(content).contains("idx_topic_metrics_topic_id");
            assertThat(content).contains("idx_trend_scores_topic_id");
            assertThat(content).contains("idx_audience_interests_user_id");
            assertThat(content).contains("idx_recommendations_user_id");
            assertThat(content).contains("idx_calendar_items_user_id");
            assertThat(content).contains("idx_notifications_user_id");
            assertThat(content).contains("idx_audit_logs_user_id");
        }

        // V4 Migration (Clustering)
        ClassPathResource v4 = new ClassPathResource("db/migration/V4__phase_3h_clustering_schema.sql");
        assertThat(v4.exists()).isTrue();

        // V5 Migration (Recommendations)
        ClassPathResource v5 = new ClassPathResource("db/migration/V5__phase_3i_recommendation_engine.sql");
        assertThat(v5.exists()).isTrue();

        // V6 Migration (Content Calendar & Planning)
        ClassPathResource v6 = new ClassPathResource("db/migration/V6__phase_3j_content_calendar.sql");
        assertThat(v6.exists()).isTrue();
        try (InputStream is = v6.getInputStream()) {
            String content = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            assertThat(content).contains("scheduled_start");
            assertThat(content).contains("scheduled_end");
            assertThat(content).contains("timezone");
            assertThat(content).contains("idx_calendar_items_user_scheduled_start");
        }

        // V7 Migration (Phase 3K Content Production Copilot)
        ClassPathResource v7 = new ClassPathResource("db/migration/V7__phase_3k_content_production.sql");
        assertThat(v7.exists()).isTrue();
        try (InputStream is = v7.getInputStream()) {
            String content = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            assertThat(content).contains("content_production_assets");
            assertThat(content).contains("recommendation_id");
            assertThat(content).contains("calendar_item_id");
            assertThat(content).contains("asset_type");
            assertThat(content).contains("generation_mode");
            assertThat(content).contains("idx_prod_assets_user_id");
        }

        // V8 Migration (Phase 3L Multi-Format Content Exporter)
        ClassPathResource v8 = new ClassPathResource("db/migration/V8__phase_3l_content_exports.sql");
        assertThat(v8.exists()).isTrue();
        try (InputStream is = v8.getInputStream()) {
            String content = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            assertThat(content).contains("content_exports");
            assertThat(content).contains("production_asset_id");
            assertThat(content).contains("export_type");
            assertThat(content).contains("content_hash");
            assertThat(content).contains("idx_content_exports_user_id");
        }

        // V9 Migration (Phase 3M Research Experiments & Evaluation)
        ClassPathResource v9 = new ClassPathResource("db/migration/V9__phase_3m_research_experiments.sql");
        assertThat(v9.exists()).isTrue();
        try (InputStream is = v9.getInputStream()) {
            String content = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            assertThat(content).contains("dataset_snapshots");
            assertThat(content).contains("evaluation_records");
            assertThat(content).contains("experiment_runs");
        }
    }
}

