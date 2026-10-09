package com.pulsegpt.comment.controller;

import com.pulsegpt.comment.dto.PostQuery;
import com.pulsegpt.comment.dto.PostResponse;
import com.pulsegpt.comment.service.PostService;
import com.pulsegpt.common.ApiResponse;
import com.pulsegpt.common.dto.PageResponse;
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

import java.util.UUID;

@RestController
@RequestMapping("/posts")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Posts", description = "Endpoints for retrieving published posts and videos")
public class PostController {

    private final PostService postService;
    private final CurrentUserService currentUserService;

    @GetMapping
    @Operation(summary = "Get paginated posts with optional filtering and search")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Posts retrieved"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid query parameters"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<ApiResponse<PageResponse<PostResponse>>> getPosts(@Valid @ModelAttribute PostQuery query) {
        User currentUser = currentUserService.requireUser();
        PageResponse<PostResponse> posts = postService.getPosts(query, currentUser);
        return ResponseEntity.ok(ApiResponse.ok("Posts retrieved successfully", posts));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a post by ID")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Post retrieved"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Post not found or belongs to another user")
    })
    public ResponseEntity<ApiResponse<PostResponse>> getPostById(@PathVariable UUID id) {
        User currentUser = currentUserService.requireUser();
        PostResponse post = postService.getPostById(id, currentUser);
        return ResponseEntity.ok(ApiResponse.ok("Post retrieved successfully", post));
    }
}
