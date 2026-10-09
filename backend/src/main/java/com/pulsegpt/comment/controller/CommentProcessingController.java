package com.pulsegpt.comment.controller;

import com.pulsegpt.common.ApiResponse;
import com.pulsegpt.comment.dto.CommentProcessingSummaryResponse;
import com.pulsegpt.comment.dto.CommentProcessingTriggerRequest;
import com.pulsegpt.comment.service.CommentProcessingService;
import com.pulsegpt.common.exception.ApiException;
import com.pulsegpt.security.CurrentUserService;
import com.pulsegpt.security.RateLimitingService;
import com.pulsegpt.user.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/processing")
@RequiredArgsConstructor
@Tag(name = "Comment Processing", description = "Endpoints for triggering NLP, sentiment, intent and embedding processing")
public class CommentProcessingController {

    private final CommentProcessingService commentProcessingService;
    private final CurrentUserService currentUserService;
    private final RateLimitingService rateLimitingService;

    @PostMapping("/comments")
    @PreAuthorize("hasAnyRole('CREATOR', 'ADMIN')")
    @Operation(summary = "Process imported raw comments", description = "Triggers the AI processing pipeline (cleaning, language, spam, PII, sentiment, intent, embeddings) for unprocessed comments")
    public ResponseEntity<ApiResponse<CommentProcessingSummaryResponse>> processComments(
            @Valid @RequestBody(required = false) CommentProcessingTriggerRequest request
    ) {
        User currentUser = currentUserService.requireUser();

        if (!rateLimitingService.tryConsumeAi(currentUser.getId())) {
            throw new ApiException("AI processing rate limit exceeded", HttpStatus.TOO_MANY_REQUESTS, "RATE_LIMIT_EXCEEDED");
        }

        CommentProcessingTriggerRequest triggerRequest = request != null ? request : new CommentProcessingTriggerRequest();
        CommentProcessingSummaryResponse response = commentProcessingService.processComments(currentUser, triggerRequest);
        return ResponseEntity.ok(ApiResponse.ok("Comments processed successfully", response));
    }
}
