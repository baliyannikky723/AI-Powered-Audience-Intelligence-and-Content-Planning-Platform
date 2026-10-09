package com.pulsegpt.comment.controller;

import com.pulsegpt.comment.dto.*;
import com.pulsegpt.comment.service.CommentService;
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
@RequestMapping("/comments")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Comments", description = "Endpoints for retrieving raw and processed comments with sentiment and intent metadata")
public class CommentController {

    private final CommentService commentService;
    private final CurrentUserService currentUserService;

    @GetMapping
    @Operation(summary = "Get paginated raw comments with optional filtering and search")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Raw comments retrieved"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid query parameters"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<ApiResponse<PageResponse<RawCommentResponse>>> getRawComments(@Valid @ModelAttribute CommentQuery query) {
        User currentUser = currentUserService.requireUser();
        PageResponse<RawCommentResponse> comments = commentService.getRawComments(query, currentUser);
        return ResponseEntity.ok(ApiResponse.ok("Comments retrieved successfully", comments));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a raw comment by ID")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Comment retrieved"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Comment not found or belongs to another user")
    })
    public ResponseEntity<ApiResponse<RawCommentResponse>> getRawCommentById(@PathVariable UUID id) {
        User currentUser = currentUserService.requireUser();
        RawCommentResponse comment = commentService.getRawCommentById(id, currentUser);
        return ResponseEntity.ok(ApiResponse.ok("Comment retrieved successfully", comment));
    }

    @GetMapping("/processed")
    @Operation(summary = "Get paginated processed comments with NLP metadata (sentiment, intent, spam, priority)")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Processed comments retrieved"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid query parameters"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<ApiResponse<PageResponse<ProcessedCommentResponse>>> getProcessedComments(@Valid @ModelAttribute ProcessedCommentQuery query) {
        User currentUser = currentUserService.requireUser();
        PageResponse<ProcessedCommentResponse> processed = commentService.getProcessedComments(query, currentUser);
        return ResponseEntity.ok(ApiResponse.ok("Processed comments retrieved successfully", processed));
    }

    @GetMapping("/processed/{id}")
    @Operation(summary = "Get a processed comment by ID")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Processed comment retrieved"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Processed comment not found or belongs to another user")
    })
    public ResponseEntity<ApiResponse<ProcessedCommentResponse>> getProcessedCommentById(@PathVariable UUID id) {
        User currentUser = currentUserService.requireUser();
        ProcessedCommentResponse processed = commentService.getProcessedCommentById(id, currentUser);
        return ResponseEntity.ok(ApiResponse.ok("Processed comment retrieved successfully", processed));
    }
}
