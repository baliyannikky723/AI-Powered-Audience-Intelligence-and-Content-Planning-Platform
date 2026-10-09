package com.pulsegpt.memory.controller;

import com.pulsegpt.audit.AuditService;
import com.pulsegpt.common.exception.ApiException;
import com.pulsegpt.common.exception.ResourceNotFoundException;
import com.pulsegpt.graph.service.KnowledgeGraphProjectionService;
import com.pulsegpt.graph.service.KnowledgeGraphQueryService;
import com.pulsegpt.memory.AudienceInterest;
import com.pulsegpt.memory.AudienceInterestRepository;
import com.pulsegpt.memory.dto.*;
import com.pulsegpt.security.CurrentUserService;
import com.pulsegpt.security.RateLimitingService;
import com.pulsegpt.topic.Topic;
import com.pulsegpt.topic.TopicRepository;
import com.pulsegpt.user.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/memory")
@RequiredArgsConstructor
@Tag(name = "Audience Memory & Knowledge Graph", description = "Endpoints for audience memory, graph signals, recurring questions, and knowledge graph rebuilds")
public class AudienceMemoryController {

    private final CurrentUserService currentUserService;
    private final KnowledgeGraphQueryService graphQueryService;
    private final KnowledgeGraphProjectionService graphProjectionService;
    private final AudienceInterestRepository interestRepository;
    private final TopicRepository topicRepository;
    private final RateLimitingService rateLimitingService;
    private final AuditService auditService;

    @GetMapping("/summary")
    @Operation(summary = "Get audience memory summary", description = "Retrieves active interests, weakening interests, recurring questions, and graph counts")
    public ResponseEntity<AudienceMemorySummaryResponse> getSummary() {
        User user = currentUserService.requireUser();
        AudienceMemorySummaryResponse summary = graphQueryService.getAudienceGraphSummary(user.getId());
        return ResponseEntity.ok(summary);
    }

    @GetMapping("/interests")
    @Operation(summary = "Get audience interests", description = "Retrieves active and weakening audience interests from memory")
    public ResponseEntity<List<AudienceInterestResponse>> getInterests(
            @RequestParam(required = false) String status) {
        User user = currentUserService.requireUser();
        List<AudienceInterestResponse> interests;
        if ("WEAKENING".equalsIgnoreCase(status)) {
            interests = graphQueryService.getWeakeningAudienceSignals(user.getId());
        } else if ("ALL".equalsIgnoreCase(status)) {
            interests = graphQueryService.getStrongestAudienceSignals(user.getId());
        } else {
            interests = graphQueryService.getActiveInterests(user.getId());
        }
        return ResponseEntity.ok(interests);
    }

    @GetMapping("/interests/{topicId}")
    @Operation(summary = "Get audience interest by topic ID", description = "Retrieves memory confidence, evidence, and decay status for a specific topic")
    public ResponseEntity<AudienceInterestResponse> getInterestByTopic(@PathVariable UUID topicId) {
        User user = currentUserService.requireUser();
        Topic topic = topicRepository.findByIdAndUserId(topicId, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Topic not found: " + topicId));

        AudienceInterest interest = interestRepository.findByUserIdAndTopicId(user.getId(), topicId)
                .orElseGet(() -> AudienceInterest.builder()
                        .user(user)
                        .topic(topic)
                        .confidence(0.85)
                        .evidenceCount(topic.getCommentCount())
                        .lastSeenAt(topic.getUpdatedAt() != null ? topic.getUpdatedAt() : topic.getCreatedAt())
                        .halfLifeDays(45)
                        .status(topic.isActive() ? com.pulsegpt.memory.AudienceInterestStatus.ACTIVE : com.pulsegpt.memory.AudienceInterestStatus.INACTIVE)
                        .build());

        AudienceInterestResponse response = AudienceInterestResponse.builder()
                .id(interest.getId() != null ? interest.getId() : topic.getId())
                .topicId(topic.getId())
                .topicName(topic.getName())
                .confidence(Math.round(interest.getConfidence() * 100.0) / 100.0)
                .evidenceCount(interest.getEvidenceCount())
                .lastSeenAt(interest.getLastSeenAt())
                .status(interest.getStatus())
                .halfLifeDays(interest.getHalfLifeDays())
                .trend(interest.getConfidence() >= 0.7 ? "GROWING" : (interest.getConfidence() >= 0.4 ? "STABLE" : "WEAKENING"))
                .evidenceIds(List.of("topic:" + topic.getId()))
                .build();

        return ResponseEntity.ok(response);
    }

    @GetMapping("/questions")
    @Operation(summary = "Get recurring audience questions", description = "Retrieves consolidated, deduplicated questions from audience memory")
    public ResponseEntity<List<AudienceQuestionResponse>> getQuestions() {
        User user = currentUserService.requireUser();
        List<AudienceQuestionResponse> questions = graphQueryService.getRecurringQuestions(user.getId());
        return ResponseEntity.ok(questions);
    }

    @GetMapping("/topics/{topicId}/related")
    @Operation(summary = "Get related topics", description = "Retrieves related topics based on knowledge graph co-occurrences and keyword links")
    public ResponseEntity<List<RelatedTopicResponse>> getRelatedTopics(@PathVariable UUID topicId) {
        User user = currentUserService.requireUser();
        topicRepository.findByIdAndUserId(topicId, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Topic not found: " + topicId));

        List<RelatedTopicResponse> related = graphQueryService.getRelatedTopics(user.getId(), topicId);
        return ResponseEntity.ok(related);
    }

    @GetMapping("/topics/{topicId}/content-ideas")
    @Operation(summary = "Get content ideas for topic", description = "Retrieves content ideas and recommendations linked to the topic in the knowledge graph")
    public ResponseEntity<List<TopicContentIdeaResponse>> getTopicContentIdeas(@PathVariable UUID topicId) {
        User user = currentUserService.requireUser();
        topicRepository.findByIdAndUserId(topicId, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Topic not found: " + topicId));

        List<TopicContentIdeaResponse> ideas = graphQueryService.getTopicContentIdeas(user.getId(), topicId);
        return ResponseEntity.ok(ideas);
    }

    @PostMapping("/rebuild")
    @Operation(summary = "Rebuild user knowledge graph", description = "Clears and reconstructs the authenticated user's knowledge graph projection from PostgreSQL")
    public ResponseEntity<MemoryRebuildResponse> rebuildKnowledgeGraph() {
        User user = currentUserService.requireUser();

        // Enforce rate limiting for sync/rebuild operations
        if (!rateLimitingService.tryConsumeSync(user.getId())) {
            throw new ApiException("Memory rebuild rate limit exceeded. Please wait before rebuilding again.",
                    HttpStatus.TOO_MANY_REQUESTS, "RATE_LIMIT_EXCEEDED");
        }

        auditService.logAuditEvent(user, "MEMORY_REBUILD_REQUESTED", user.getId().toString(), Map.of("mode", "FULL_REBUILD"));

        MemoryRebuildResponse response = graphProjectionService.rebuildUserGraph(user.getId());
        return ResponseEntity.ok(response);
    }
}
