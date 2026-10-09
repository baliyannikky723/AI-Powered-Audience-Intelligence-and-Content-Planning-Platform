package com.pulsegpt.comment.service;

import com.pulsegpt.comment.Post;
import com.pulsegpt.comment.PostRepository;
import com.pulsegpt.comment.dto.PostQuery;
import com.pulsegpt.comment.dto.PostResponse;
import com.pulsegpt.comment.mapper.PostMapper;
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
public class PostService {

    private final PostRepository postRepository;
    private final PostMapper postMapper;

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "publishedAt", "createdAt", "viewsCount", "likesCount", "commentsCount", "title"
    );

    @Transactional(readOnly = true)
    public PageResponse<PostResponse> getPosts(PostQuery query, User currentUser) {
        PaginationUtils.validateDateRange(query.from(), query.to());
        String cleanSearch = PaginationUtils.cleanSearchTerm(query.search());

        Pageable pageable = PaginationUtils.createPageRequest(
                query.getPageOrDefault(),
                query.getSizeOrDefault(),
                query.sort(),
                query.direction(),
                "publishedAt",
                ALLOWED_SORT_FIELDS
        );

        Specification<Post> spec = buildPostSpecification(query, cleanSearch, currentUser.getId());
        Page<Post> page = postRepository.findAll(spec, pageable);
        List<PostResponse> responses = postMapper.toResponseList(page.getContent());

        return PageResponse.from(page, responses);
    }

    @Transactional(readOnly = true)
    public PostResponse getPostById(UUID postId, User currentUser) {
        Post post = postRepository.findByIdAndPlatformAccountUserId(postId, currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Post not found with id: " + postId));
        return postMapper.toResponse(post);
    }

    private Specification<Post> buildPostSpecification(PostQuery query, String cleanSearch, UUID userId) {
        return (root, criteriaQuery, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // 1. Mandatory User Scoping
            predicates.add(cb.equal(root.get("platformAccount").get("user").get("id"), userId));

            // 2. Platform Account Filter
            if (query.platformAccountId() != null) {
                predicates.add(cb.equal(root.get("platformAccount").get("id"), query.platformAccountId()));
            }

            // 3. Platform Type Filter
            if (query.platform() != null) {
                predicates.add(cb.equal(root.get("platformAccount").get("platform"), query.platform()));
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
                predicates.add(cb.like(cb.lower(root.get("title")), searchPattern));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
