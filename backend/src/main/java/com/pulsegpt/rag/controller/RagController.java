package com.pulsegpt.rag.controller;

import com.pulsegpt.platform.PlatformType;
import com.pulsegpt.rag.dto.*;
import com.pulsegpt.rag.model.RagMode;
import com.pulsegpt.rag.service.RagEvaluationService;
import com.pulsegpt.rag.service.RagQueryService;
import com.pulsegpt.security.CurrentUserService;
import com.pulsegpt.user.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/rag")
@RequiredArgsConstructor
public class RagController {

    private final RagQueryService ragQueryService;
    private final RagEvaluationService ragEvaluationService;
    private final CurrentUserService currentUserService;

    @PostMapping("/query")
    public ResponseEntity<RagQueryResponse> executeRagQuery(@Valid @RequestBody RagQueryRequest request) {
        User user = currentUserService.requireUser();
        log.info("RAG query received from user {}: mode={}, query='{}'",
                user.getId(), request.generationMode(), request.query());

        PlatformType platform = null;
        if (request.platform() != null && !request.platform().isBlank()) {
            try {
                platform = PlatformType.valueOf(request.platform().toUpperCase());
            } catch (Exception ignored) {}
        }

        RagQuery query = RagQuery.builder()
                .queryText(request.query())
                .userId(user.getId())
                .ragMode(RagMode.fromString(request.generationMode()))
                .platform(platform)
                .topicId(request.topicId())
                .maxEvidence(request.maxEvidence())
                .timeRangeDays(request.timeRangeDays())
                .includeMemory(true)
                .includeQuestions(true)
                .includeTrends(true)
                .includeContentHistory(true)
                .build();

        RagQueryResponse response = ragQueryService.executeRagQuery(user, query);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/retrieve")
    public ResponseEntity<RagRetrieveResponse> retrieveEvidenceOnly(@Valid @RequestBody RagQueryRequest request) {
        User user = currentUserService.requireUser();
        log.info("RAG retrieve-only request from user {}: query='{}'", user.getId(), request.query());

        PlatformType platform = null;
        if (request.platform() != null && !request.platform().isBlank()) {
            try {
                platform = PlatformType.valueOf(request.platform().toUpperCase());
            } catch (Exception ignored) {}
        }

        RagQuery query = RagQuery.builder()
                .queryText(request.query())
                .userId(user.getId())
                .ragMode(RagMode.fromString(request.generationMode()))
                .platform(platform)
                .topicId(request.topicId())
                .maxEvidence(request.maxEvidence())
                .timeRangeDays(request.timeRangeDays())
                .includeMemory(true)
                .includeQuestions(true)
                .includeTrends(true)
                .includeContentHistory(true)
                .build();

        RagRetrieveResponse response = ragQueryService.retrieveEvidenceOnly(user, query);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/annotations")
    public ResponseEntity<EvidenceAnnotationResponse> createAnnotation(
            @Valid @RequestBody EvidenceAnnotationRequest request) {
        User user = currentUserService.requireUser();
        EvidenceAnnotationResponse response = ragEvaluationService.createAnnotation(user, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/annotations")
    public ResponseEntity<Page<EvidenceAnnotationResponse>> listAnnotations(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        User user = currentUserService.requireUser();
        Page<EvidenceAnnotationResponse> response = ragEvaluationService.getUserAnnotations(user, pageable);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/evaluation")
    public ResponseEntity<RagEvaluationMetricsResponse> getEvaluationMetrics() {
        User user = currentUserService.requireUser();
        RagEvaluationMetricsResponse response = ragEvaluationService.getEvaluationMetrics(user);
        return ResponseEntity.ok(response);
    }
}
