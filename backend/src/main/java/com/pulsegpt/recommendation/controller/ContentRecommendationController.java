package com.pulsegpt.recommendation.controller;

import com.pulsegpt.common.ApiResponse;
import com.pulsegpt.common.dto.PageResponse;
import com.pulsegpt.recommendation.dto.RecommendationGenerateRequest;
import com.pulsegpt.recommendation.dto.RecommendationQuery;
import com.pulsegpt.recommendation.dto.RecommendationResponse;
import com.pulsegpt.recommendation.service.ContentRecommendationService;
import com.pulsegpt.security.CurrentUserService;
import com.pulsegpt.user.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/recommendations")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Recommendations", description = "Endpoints for evidence-grounded content recommendations, 6-check validation, and audit tracking")
public class ContentRecommendationController {

    private final ContentRecommendationService recommendationService;
    private final CurrentUserService currentUserService;

    @PostMapping("/generate")
    @Operation(summary = "Generate evidence-grounded content recommendations from audience intelligence")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Recommendations generated, validated, and persisted"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid generation request parameters"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "429", description = "AI rate limit exceeded"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "503", description = "AI Microservice / LLM unavailable")
    })
    public ResponseEntity<ApiResponse<List<RecommendationResponse>>> generateRecommendations(
            @Valid @RequestBody RecommendationGenerateRequest request
    ) {
        User currentUser = currentUserService.requireUser();
        List<RecommendationResponse> responses = recommendationService.generateRecommendations(currentUser, request);
        return ResponseEntity.ok(ApiResponse.ok("Recommendations generated successfully", responses));
    }

    @GetMapping
    @Operation(summary = "Get paginated recommendations with filtering by status, topic, or content type")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Recommendations retrieved"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid query parameters"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<ApiResponse<PageResponse<RecommendationResponse>>> getRecommendations(
            @Valid @ModelAttribute RecommendationQuery query
    ) {
        User currentUser = currentUserService.requireUser();
        PageResponse<RecommendationResponse> responses = recommendationService.getRecommendations(currentUser, query);
        return ResponseEntity.ok(ApiResponse.ok("Recommendations retrieved successfully", responses));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a single recommendation by ID with evidence snapshot and validation report")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Recommendation details retrieved"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Recommendation not found or unauthorized")
    })
    public ResponseEntity<ApiResponse<RecommendationResponse>> getRecommendationById(@PathVariable UUID id) {
        User currentUser = currentUserService.requireUser();
        RecommendationResponse response = recommendationService.getRecommendationById(currentUser, id);
        return ResponseEntity.ok(ApiResponse.ok("Recommendation retrieved successfully", response));
    }

    @PostMapping("/{id}/approve")
    @Operation(summary = "Explicitly approve a validated content recommendation for creator-controlled scheduling")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Recommendation approved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Recommendation is not in VALIDATED state"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Recommendation not found or unauthorized")
    })
    public ResponseEntity<ApiResponse<RecommendationResponse>> approveRecommendation(@PathVariable UUID id) {
        User currentUser = currentUserService.requireUser();
        RecommendationResponse response = recommendationService.approveRecommendation(currentUser, id);
        return ResponseEntity.ok(ApiResponse.ok("Recommendation approved successfully", response));
    }
}
