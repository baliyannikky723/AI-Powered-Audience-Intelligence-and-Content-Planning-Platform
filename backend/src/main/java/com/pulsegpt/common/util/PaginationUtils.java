package com.pulsegpt.common.util;

import com.pulsegpt.common.exception.ValidationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.time.Instant;
import java.util.Set;

public final class PaginationUtils {

    private PaginationUtils() {}

    public static Pageable createPageRequest(int page, int size, String sortField, String direction,
                                             String defaultSortField, Set<String> allowedSortFields) {
        if (page < 0) {
            throw new ValidationException("Page index must not be less than zero");
        }
        if (size < 1 || size > 100) {
            throw new ValidationException("Page size must be between 1 and 100");
        }

        String field = (sortField != null && !sortField.isBlank()) ? sortField.trim() : defaultSortField;
        if (!allowedSortFields.contains(field)) {
            throw new ValidationException("Invalid sort field: '" + field + "'. Allowed fields are: " + allowedSortFields);
        }

        Sort.Direction dir = "ASC".equalsIgnoreCase(direction) ? Sort.Direction.ASC : Sort.Direction.DESC;

        // Deterministic sorting with secondary ID sort
        Sort sort = Sort.by(new Sort.Order(dir, field), new Sort.Order(Sort.Direction.DESC, "id"));
        return PageRequest.of(page, size, sort);
    }

    public static void validateDateRange(Instant from, Instant to) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new ValidationException("The 'from' timestamp cannot be after 'to' timestamp");
        }
    }

    public static String cleanSearchTerm(String search) {
        if (search == null || search.isBlank()) {
            return null;
        }
        String trimmed = search.trim();
        if (trimmed.length() > 200) {
            throw new ValidationException("Search query exceeds maximum allowed length of 200 characters");
        }
        return trimmed;
    }
}
