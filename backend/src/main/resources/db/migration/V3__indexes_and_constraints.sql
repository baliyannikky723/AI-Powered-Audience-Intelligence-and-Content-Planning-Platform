-- =============================================================================
-- PulseGPT Database Migration V3: Indexes and Constraints
-- =============================================================================

-- 1. Users Indexes & Constraints
CREATE INDEX IF NOT EXISTS idx_users_email ON users(email);
CREATE INDEX IF NOT EXISTS idx_users_role ON users(role);
CREATE INDEX IF NOT EXISTS idx_users_created_at ON users(created_at);

-- 2. Refresh Tokens Indexes
CREATE INDEX IF NOT EXISTS idx_refresh_tokens_user_id ON refresh_tokens(user_id);
CREATE INDEX IF NOT EXISTS idx_refresh_tokens_token_hash ON refresh_tokens(token_hash);
CREATE INDEX IF NOT EXISTS idx_refresh_tokens_expires_at ON refresh_tokens(expires_at);

-- 3. Platform Accounts Indexes & Constraints
ALTER TABLE platform_accounts
    ADD CONSTRAINT uq_platform_account_user_platform_ext
    UNIQUE (user_id, platform, external_account_id);

CREATE INDEX IF NOT EXISTS idx_platform_accounts_user_id ON platform_accounts(user_id);
CREATE INDEX IF NOT EXISTS idx_platform_accounts_platform ON platform_accounts(platform);
CREATE INDEX IF NOT EXISTS idx_platform_accounts_status ON platform_accounts(status);

-- 4. Posts Indexes & Constraints
ALTER TABLE posts
    ADD CONSTRAINT uq_posts_platform_account_ext_id
    UNIQUE (platform_account_id, external_post_id);

CREATE INDEX IF NOT EXISTS idx_posts_platform_account_id ON posts(platform_account_id);
CREATE INDEX IF NOT EXISTS idx_posts_external_post_id ON posts(external_post_id);
CREATE INDEX IF NOT EXISTS idx_posts_published_at ON posts(published_at);

-- 5. Ingestion Runs Indexes
CREATE INDEX IF NOT EXISTS idx_ingestion_runs_platform_account ON ingestion_runs(platform_account_id);
CREATE INDEX IF NOT EXISTS idx_ingestion_runs_status ON ingestion_runs(status);
CREATE INDEX IF NOT EXISTS idx_ingestion_runs_started_at ON ingestion_runs(started_at);

-- 6. Raw Comments Indexes & Constraints
ALTER TABLE raw_comments
    ADD CONSTRAINT uq_raw_comments_post_ext_id
    UNIQUE (post_id, external_comment_id);

CREATE INDEX IF NOT EXISTS idx_raw_comments_post_id ON raw_comments(post_id);
CREATE INDEX IF NOT EXISTS idx_raw_comments_external_comment_id ON raw_comments(external_comment_id);
CREATE INDEX IF NOT EXISTS idx_raw_comments_published_at ON raw_comments(published_at);

-- 7. Processed Comments Indexes
CREATE INDEX IF NOT EXISTS idx_processed_comments_raw_id ON processed_comments(raw_comment_id);
CREATE INDEX IF NOT EXISTS idx_processed_comments_language ON processed_comments(language);
CREATE INDEX IF NOT EXISTS idx_processed_comments_sentiment ON processed_comments(sentiment_label);
CREATE INDEX IF NOT EXISTS idx_processed_comments_intent ON processed_comments(intent);
CREATE INDEX IF NOT EXISTS idx_processed_comments_priority ON processed_comments(priority);
CREATE INDEX IF NOT EXISTS idx_processed_comments_processed_at ON processed_comments(processed_at);

-- 8. Topics Indexes
CREATE INDEX IF NOT EXISTS idx_topics_user_id ON topics(user_id);
CREATE INDEX IF NOT EXISTS idx_topics_name ON topics(name);
CREATE INDEX IF NOT EXISTS idx_topics_active ON topics(active);

-- 9. Topic Metrics Daily Constraints & Indexes
ALTER TABLE topic_metrics_daily
    ADD CONSTRAINT uq_topic_metrics_topic_date
    UNIQUE (topic_id, metric_date);

CREATE INDEX IF NOT EXISTS idx_topic_metrics_topic_id ON topic_metrics_daily(topic_id);
CREATE INDEX IF NOT EXISTS idx_topic_metrics_metric_date ON topic_metrics_daily(metric_date);

-- 10. Trend Scores Constraints & Indexes
ALTER TABLE trend_scores
    ADD CONSTRAINT uq_trend_scores_topic_date
    UNIQUE (topic_id, metric_date);

CREATE INDEX IF NOT EXISTS idx_trend_scores_topic_id ON trend_scores(topic_id);
CREATE INDEX IF NOT EXISTS idx_trend_scores_metric_date ON trend_scores(metric_date);
CREATE INDEX IF NOT EXISTS idx_trend_scores_status ON trend_scores(status);

-- 11. Audience Interests Constraints & Indexes
ALTER TABLE audience_interests
    ADD CONSTRAINT uq_audience_interests_user_topic
    UNIQUE (user_id, topic_id);

CREATE INDEX IF NOT EXISTS idx_audience_interests_user_id ON audience_interests(user_id);
CREATE INDEX IF NOT EXISTS idx_audience_interests_topic_id ON audience_interests(topic_id);
CREATE INDEX IF NOT EXISTS idx_audience_interests_status ON audience_interests(status);

-- 12. Content Recommendations Indexes
CREATE INDEX IF NOT EXISTS idx_recommendations_user_id ON content_recommendations(user_id);
CREATE INDEX IF NOT EXISTS idx_recommendations_topic_id ON content_recommendations(topic_id);
CREATE INDEX IF NOT EXISTS idx_recommendations_created_at ON content_recommendations(created_at);
CREATE INDEX IF NOT EXISTS idx_recommendations_priority ON content_recommendations(priority);

-- 13. Calendar Items Indexes
CREATE INDEX IF NOT EXISTS idx_calendar_items_user_id ON calendar_items(user_id);
CREATE INDEX IF NOT EXISTS idx_calendar_items_scheduled_at ON calendar_items(scheduled_at);
CREATE INDEX IF NOT EXISTS idx_calendar_items_status ON calendar_items(status);

-- 14. Chat Sessions & Messages Indexes
CREATE INDEX IF NOT EXISTS idx_chat_sessions_user_id ON chat_sessions(user_id);
CREATE INDEX IF NOT EXISTS idx_chat_messages_session_id ON chat_messages(session_id);
CREATE INDEX IF NOT EXISTS idx_chat_messages_created_at ON chat_messages(created_at);

-- 15. Notifications Indexes
CREATE INDEX IF NOT EXISTS idx_notifications_user_id ON notifications(user_id);
CREATE INDEX IF NOT EXISTS idx_notifications_read_at ON notifications(read_at);
CREATE INDEX IF NOT EXISTS idx_notifications_created_at ON notifications(created_at);

-- 16. Audit Logs Indexes
CREATE INDEX IF NOT EXISTS idx_audit_logs_user_id ON audit_logs(user_id);
CREATE INDEX IF NOT EXISTS idx_audit_logs_correlation_id ON audit_logs(correlation_id);
CREATE INDEX IF NOT EXISTS idx_audit_logs_created_at ON audit_logs(created_at);

-- 17. API Quota Usage Constraints & Indexes
ALTER TABLE api_quota_usage
    ADD CONSTRAINT uq_api_quota_account_date
    UNIQUE (platform_account_id, usage_date);

CREATE INDEX IF NOT EXISTS idx_api_quota_account_id ON api_quota_usage(platform_account_id);
CREATE INDEX IF NOT EXISTS idx_api_quota_usage_date ON api_quota_usage(usage_date);

-- 18. Annotations Indexes
CREATE INDEX IF NOT EXISTS idx_annotations_processed_id ON annotations(processed_comment_id);
CREATE INDEX IF NOT EXISTS idx_annotations_label_type ON annotations(label_type);
