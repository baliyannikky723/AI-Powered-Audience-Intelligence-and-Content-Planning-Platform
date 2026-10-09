package com.pulsegpt.ai.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

import java.util.List;
import java.util.Map;

@Builder
public record AiClusterTopicResult(
        @JsonProperty("cluster_id") Integer clusterId,
        String label,
        List<AiClusterKeywordScore> keywords,
        @JsonProperty("raw_keywords") List<String> rawKeywords,
        @JsonProperty("comment_count") Integer commentCount,
        List<Double> centroid,
        @JsonProperty("sentiment_distribution") Map<String, Integer> sentimentDistribution,
        @JsonProperty("intent_distribution") Map<String, Integer> intentDistribution,
        @JsonProperty("language_distribution") Map<String, Integer> languageDistribution,
        @JsonProperty("platform_distribution") Map<String, Integer> platformDistribution,
        @JsonProperty("exemplar_comment_ids") List<String> exemplarCommentIds
) {}
