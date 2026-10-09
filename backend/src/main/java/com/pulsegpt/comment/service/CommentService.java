package com.pulsegpt.comment.service;

import com.pulsegpt.comment.ProcessedComment;
import com.pulsegpt.comment.ProcessedCommentRepository;
import com.pulsegpt.comment.RawComment;
import com.pulsegpt.comment.RawCommentRepository;
import com.pulsegpt.comment.dto.*;
import com.pulsegpt.comment.mapper.ProcessedCommentMapper;
import com.pulsegpt.comment.mapper.RawCommentMapper;
import com.pulsegpt.common.dto.PageResponse;
import com.pulsegpt.common.exception.ResourceNotFoundException;
import com.pulsegpt.common.util.PaginationUtils;
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
public class CommentService {

    private final RawCommentRepository rawCommentRepository;
    private final ProcessedCommentRepository processedCommentRepository;
    private final RawCommentMapper rawCommentMapper;
    private final ProcessedCommentMapper processedCommentMapper;

    private static final Set<String> ALLOWED_RAW_SORT_FIELDS = Set.of(
            "publishedAt", "importedAt", "likes", "replies"
    );

    private static final Set<String> ALLOWED_PROCESSED_SORT_FIELDS = Set.of(
            "processedAt", "sentimentScore", "spamScore", "priority", "language"
    );

    @Transactional(readOnly = true)
    public PageResponse<RawCommentResponse> getRawComments(CommentQuery query, User currentUser) {
        PaginationUtils.validateDateRange(query.from(), query.to());
        String cleanSearch = PaginationUtils.cleanSearchTerm(query.search());

        Pageable pageable = PaginationUtils.createPageRequest(
                query.getPageOrDefault(),
                query.getSizeOrDefault(),
                query.sort(),
                query.direction(),
                "publishedAt",
                ALLOWED_RAW_SORT_FIELDS
        );

        Specification<RawComment> spec = buildRawCommentSpecification(query, cleanSearch, currentUser.getId());
        Page<RawComment> page = rawCommentRepository.findAll(spec, pageable);
        List<RawCommentResponse> responses = rawCommentMapper.toResponseList(page.getContent());

        return PageResponse.from(page, responses);
    }

    @Transactional(readOnly = true)
    public RawCommentResponse getRawCommentById(UUID commentId, User currentUser) {
        RawComment rawComment = rawCommentRepository.findByIdAndPostPlatformAccountUserId(commentId, currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Comment not found with id: " + commentId));
        return rawCommentMapper.toResponse(rawComment);
    }

    @Transactional(readOnly = true)
    public PageResponse<ProcessedCommentResponse> getProcessedComments(ProcessedCommentQuery query, User currentUser) {
        PaginationUtils.validateDateRange(query.from(), query.to());
        String cleanSearch = PaginationUtils.cleanSearchTerm(query.search());

        Pageable pageable = PaginationUtils.createPageRequest(
                query.getPageOrDefault(),
                query.getSizeOrDefault(),
                query.sort(),
                query.direction(),
                "processedAt",
                ALLOWED_PROCESSED_SORT_FIELDS
        );

        Specification<ProcessedComment> spec = buildProcessedCommentSpecification(query, cleanSearch, currentUser.getId());
        Page<ProcessedComment> page = processedCommentRepository.findAll(spec, pageable);
        List<ProcessedCommentResponse> responses = processedCommentMapper.toResponseList(page.getContent());

        return PageResponse.from(page, responses);
    }

    @Transactional(readOnly = true)
    public ProcessedCommentResponse getProcessedCommentById(UUID processedCommentId, User currentUser) {
        ProcessedComment processedComment = processedCommentRepository.findByIdAndRawCommentPostPlatformAccountUserId(processedCommentId, currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Processed comment not found with id: " + processedCommentId));
        return processedCommentMapper.toResponse(processedComment);
    }

    private Specification<RawComment> buildRawCommentSpecification(CommentQuery query, String cleanSearch, UUID userId) {
        return (root, criteriaQuery, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // 1. Mandatory User Scoping
            predicates.add(cb.equal(root.get("post").get("platformAccount").get("user").get("id"), userId));

            // 2. Post Filter
            if (query.postId() != null) {
                predicates.add(cb.equal(root.get("post").get("id"), query.postId()));
            }

            // 3. Platform Filter
            if (query.platform() != null) {
                predicates.add(cb.equal(root.get("post").get("platformAccount").get("platform"), query.platform()));
            }

            // 4. Date Range Filter
            if (query.from() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("publishedAt"), query.from()));
            }
            if (query.to() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("publishedAt"), query.to()));
            }

            // 5. Search Filter
            if (cleanSearch != null) {
                String searchPattern = "%" + cleanSearch.toLowerCase() + "%";
                predicates.add(cb.like(cb.lower(root.get("rawText")), searchPattern));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private Specification<ProcessedComment> buildProcessedCommentSpecification(ProcessedCommentQuery query, String cleanSearch, UUID userId) {
        return (root, criteriaQuery, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // 1. Mandatory User Scoping
            predicates.add(cb.equal(root.get("rawComment").get("post").get("platformAccount").get("user").get("id"), userId));

            // 2. Post Filter
            if (query.postId() != null) {
                predicates.add(cb.equal(root.get("rawComment").get("post").get("id"), query.postId()));
            }

            // 3. Platform Filter
            if (query.platform() != null) {
                predicates.add(cb.equal(root.get("rawComment").get("post").get("platformAccount").get("platform"), query.platform()));
            }

            // 4. Sentiment Filter
            if (query.sentiment() != null) {
                predicates.add(cb.equal(root.get("sentimentLabel"), query.sentiment()));
            }

            // 5. Intent Filter
            if (query.intent() != null) {
                predicates.add(cb.equal(root.get("intent"), query.intent()));
            }

            // 6. Priority Filter
            if (query.priority() != null) {
                predicates.add(cb.equal(root.get("priority"), query.priority()));
            }

            // 7. Language Filter
            if (query.language() != null && !query.language().isBlank()) {
                predicates.add(cb.equal(cb.lower(root.get("language")), query.language().trim().toLowerCase()));
            }

            // 8. Spam Filter
            if (query.isSpam() != null) {
                predicates.add(cb.equal(root.get("isSpam"), query.isSpam()));
            }

            // 9. Duplicate Filter
            if (query.isDuplicate() != null) {
                predicates.add(cb.equal(root.get("isDuplicate"), query.isDuplicate()));
            }

            // 10. Date Range Filter
            if (query.from() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("processedAt"), query.from()));
            }
            if (query.to() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("processedAt"), query.to()));
            }

            // 11. Search Filter
            if (cleanSearch != null) {
                String searchPattern = "%" + cleanSearch.toLowerCase() + "%";
                predicates.add(cb.like(cb.lower(root.get("normalizedText")), searchPattern));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
