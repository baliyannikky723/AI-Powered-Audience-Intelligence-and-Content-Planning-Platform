package com.pulsegpt.topic.controller;

import com.pulsegpt.common.ApiResponse;
import com.pulsegpt.common.dto.PageResponse;
import com.pulsegpt.security.CurrentUserService;
import com.pulsegpt.topic.dto.*;
import com.pulsegpt.topic.service.AudienceClusteringService;
import com.pulsegpt.topic.service.TopicService;
import com.pulsegpt.user.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/topics")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Topics", description = "Endpoints for managing audience topics, semantic clustering runs, and topic-comment assignments")
public class TopicController {

    private final TopicService topicService;
    private final AudienceClusteringService audienceClusteringService;
    private final CurrentUserService currentUserService;

    @PostMapping("/cluster")
    @Operation(summary = "Trigger semantic audience clustering pipeline (UMAP + HDBSCAN + c-TF-IDF)")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Clustering run completed successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid clustering parameters"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "503", description = "AI Microservice unavailable")
    })
    public ResponseEntity<ApiResponse<ClusteringSummaryResponse>> triggerClustering(
            @Valid @RequestBody(required = false) ClusteringTriggerRequest request
    ) {
        User currentUser = currentUserService.requireUser();
        ClusteringTriggerRequest triggerRequest = (request != null)
                ? request
                : ClusteringTriggerRequest.builder().build();

        ClusteringSummaryResponse response = audienceClusteringService.executeClustering(currentUser, triggerRequest);
        return ResponseEntity.ok(ApiResponse.ok("Audience clustering completed successfully", response));
    }

    @GetMapping
    @Operation(summary = "Get paginated topics with optional filtering and search")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Topics retrieved"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid query parameters"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<ApiResponse<PageResponse<TopicResponse>>> getTopics(@Valid @ModelAttribute TopicQuery query) {
        User currentUser = currentUserService.requireUser();
        PageResponse<TopicResponse> topics = topicService.getTopics(query, currentUser);
        return ResponseEntity.ok(ApiResponse.ok("Topics retrieved successfully", topics));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a topic by ID")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Topic retrieved"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Topic not found or belongs to another user")
    })
    public ResponseEntity<ApiResponse<TopicResponse>> getTopicById(@PathVariable UUID id) {
        User currentUser = currentUserService.requireUser();
        TopicResponse topic = topicService.getTopicById(id, currentUser);
        return ResponseEntity.ok(ApiResponse.ok("Topic retrieved successfully", topic));
    }

    @GetMapping("/{id}/comments")
    @Operation(summary = "Get paginated comments assigned to a topic")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Topic comments retrieved"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Topic not found or belongs to another user")
    })
    public ResponseEntity<ApiResponse<PageResponse<TopicCommentResponse>>> getTopicComments(
            @PathVariable UUID id,
            @Valid @ModelAttribute TopicCommentQuery query
    ) {
        User currentUser = currentUserService.requireUser();
        PageResponse<TopicCommentResponse> comments = topicService.getTopicComments(id, query, currentUser);
        return ResponseEntity.ok(ApiResponse.ok("Topic comments retrieved successfully", comments));
    }
}
