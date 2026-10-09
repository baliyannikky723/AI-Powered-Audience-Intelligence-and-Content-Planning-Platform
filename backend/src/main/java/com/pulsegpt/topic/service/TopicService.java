package com.pulsegpt.topic.service;

import com.pulsegpt.comment.ProcessedComment;
import com.pulsegpt.comment.RawComment;
import com.pulsegpt.common.dto.PageResponse;
import com.pulsegpt.common.exception.ResourceNotFoundException;
import com.pulsegpt.common.util.PaginationUtils;
import com.pulsegpt.topic.*;
import com.pulsegpt.topic.dto.*;
import com.pulsegpt.topic.mapper.ClusteringRunMapper;
import com.pulsegpt.topic.mapper.TopicMapper;
import com.pulsegpt.user.User;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class TopicService {

    private final TopicRepository topicRepository;
    private final TopicAssignmentRepository topicAssignmentRepository;
    private final ClusteringRunRepository clusteringRunRepository;
    private final TopicMapper topicMapper;
    private final ClusteringRunMapper clusteringRunMapper;

    private static final Set<String> ALLOWED_TOPIC_SORT_FIELDS = Set.of(
            "createdAt", "updatedAt", "name", "active", "commentCount"
    );

    private static final Set<String> ALLOWED_RUN_SORT_FIELDS = Set.of(
            "startedAt", "completedAt", "createdAt", "status"
    );

    @Transactional(readOnly = true)
    public PageResponse<TopicResponse> getTopics(TopicQuery query, User currentUser) {
        String cleanSearch = PaginationUtils.cleanSearchTerm(query.search());

        Pageable pageable = PaginationUtils.createPageRequest(
                query.getPageOrDefault(),
                query.getSizeOrDefault(),
                query.sort(),
                query.direction(),
                "createdAt",
                ALLOWED_TOPIC_SORT_FIELDS
        );

        Specification<Topic> spec = buildTopicSpecification(query, cleanSearch, currentUser.getId());
        Page<Topic> page = topicRepository.findAll(spec, pageable);
        List<TopicResponse> responses = topicMapper.toResponseList(page.getContent());

        return PageResponse.from(page, responses);
    }

    @Transactional(readOnly = true)
    public TopicResponse getTopicById(UUID topicId, User currentUser) {
        Topic topic = topicRepository.findByIdAndUserId(topicId, currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Topic not found with id: " + topicId));
        return topicMapper.toResponse(topic);
    }

    @Transactional(readOnly = true)
    public PageResponse<TopicCommentResponse> getTopicComments(UUID topicId, TopicCommentQuery query, User currentUser) {
        // Verify topic exists and belongs to user
        Topic topic = topicRepository.findByIdAndUserId(topicId, currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Topic not found with id: " + topicId));

        Pageable pageable = PaginationUtils.createPageRequest(
                query.getPageOrDefault(),
                query.getSizeOrDefault(),
                query.sort(),
                query.direction(),
                "assignedAt",
                Set.of("assignedAt", "membershipProbability")
        );

        Page<TopicAssignment> assignmentPage = topicAssignmentRepository.findByTopicIdAndUserId(topic.getId(), currentUser.getId(), pageable);

        List<TopicCommentResponse> responses = assignmentPage.getContent().stream()
                .map(ta -> {
                    ProcessedComment pc = ta.getProcessedComment();
                    RawComment rc = pc.getRawComment();
                    return TopicCommentResponse.builder()
                            .assignmentId(ta.getId())
                            .processedCommentId(pc.getId())
                            .rawCommentId(rc.getId())
                            .text(pc.getNormalizedText() != null ? pc.getNormalizedText() : rc.getRawText())
                            .authorDisplayName(rc.getAuthorDisplayName())
                            .sentimentLabel(pc.getSentimentLabel())
                            .sentimentScore(pc.getSentimentScore())
                            .intent(pc.getIntent())
                            .priority(pc.getPriority())
                            .language(pc.getLanguage())
                            .isHinglish(pc.getIsHinglish())
                            .membershipProbability(ta.getMembershipProbability())
                            .platform(rc.getPost().getPlatformAccount().getPlatform())
                            .publishedAt(rc.getPublishedAt())
                            .assignedAt(ta.getAssignedAt())
                            .build();
                })
                .toList();

        return PageResponse.from(assignmentPage, responses);
    }

    @Transactional(readOnly = true)
    public PageResponse<ClusteringRunResponse> getClusteringRuns(ClusteringRunQuery query, User currentUser) {
        Pageable pageable = PaginationUtils.createPageRequest(
                query.getPageOrDefault(),
                query.getSizeOrDefault(),
                query.sort(),
                query.direction(),
                "startedAt",
                ALLOWED_RUN_SORT_FIELDS
        );

        Page<ClusteringRun> runPage = clusteringRunRepository.findByUserId(currentUser.getId(), pageable);
        List<ClusteringRunResponse> responses = clusteringRunMapper.toResponseList(runPage.getContent());

        return PageResponse.from(runPage, responses);
    }

    @Transactional(readOnly = true)
    public ClusteringRunResponse getClusteringRunById(UUID runId, User currentUser) {
        ClusteringRun run = clusteringRunRepository.findByIdAndUserId(runId, currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Clustering run not found with id: " + runId));
        return clusteringRunMapper.toResponse(run);
    }

    private Specification<Topic> buildTopicSpecification(TopicQuery query, String cleanSearch, UUID userId) {
        return (root, criteriaQuery, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // 1. Mandatory User Scoping
            predicates.add(cb.equal(root.get("user").get("id"), userId));

            // 2. Active Status Filter
            if (query.active() != null) {
                predicates.add(cb.equal(root.get("active"), query.active()));
            }

            // 3. Search Filter (by name or description)
            if (cleanSearch != null) {
                String searchPattern = "%" + cleanSearch.toLowerCase() + "%";
                Predicate nameMatch = cb.like(cb.lower(root.get("name")), searchPattern);
                Predicate descMatch = cb.like(cb.lower(root.get("description")), searchPattern);
                predicates.add(cb.or(nameMatch, descMatch));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
