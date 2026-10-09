package com.pulsegpt.topic.controller;

import com.pulsegpt.common.ApiResponse;
import com.pulsegpt.common.dto.PageResponse;
import com.pulsegpt.security.CurrentUserService;
import com.pulsegpt.topic.dto.ClusteringRunQuery;
import com.pulsegpt.topic.dto.ClusteringRunResponse;
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
@RequestMapping("/clustering/runs")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Clustering Runs", description = "Endpoints for retrieving history, metadata, and quality metrics of audience clustering runs")
public class ClusteringRunController {

    private final TopicService topicService;
    private final CurrentUserService currentUserService;

    @GetMapping
    @Operation(summary = "Get paginated clustering runs for the authenticated user")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Clustering runs retrieved"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid query parameters"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<ApiResponse<PageResponse<ClusteringRunResponse>>> getClusteringRuns(@Valid @ModelAttribute ClusteringRunQuery query) {
        User currentUser = currentUserService.requireUser();
        PageResponse<ClusteringRunResponse> runs = topicService.getClusteringRuns(query, currentUser);
        return ResponseEntity.ok(ApiResponse.ok("Clustering runs retrieved successfully", runs));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a clustering run by ID")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Clustering run retrieved"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Clustering run not found or belongs to another user")
    })
    public ResponseEntity<ApiResponse<ClusteringRunResponse>> getClusteringRunById(@PathVariable UUID id) {
        User currentUser = currentUserService.requireUser();
        ClusteringRunResponse run = topicService.getClusteringRunById(id, currentUser);
        return ResponseEntity.ok(ApiResponse.ok("Clustering run retrieved successfully", run));
    }
}
