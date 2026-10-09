package com.pulsegpt.calendar.service;

import com.pulsegpt.audit.AuditService;
import com.pulsegpt.calendar.CalendarItem;
import com.pulsegpt.calendar.CalendarItemRepository;
import com.pulsegpt.calendar.CalendarItemStatus;
import com.pulsegpt.calendar.dto.*;
import com.pulsegpt.calendar.exception.CalendarConflictException;
import com.pulsegpt.calendar.mapper.CalendarMapper;
import com.pulsegpt.comment.Priority;
import com.pulsegpt.common.dto.PageResponse;
import com.pulsegpt.common.exception.ApiException;
import com.pulsegpt.common.exception.ResourceNotFoundException;
import com.pulsegpt.common.util.PaginationUtils;
import com.pulsegpt.platform.PlatformType;
import com.pulsegpt.recommendation.ContentRecommendation;
import com.pulsegpt.recommendation.ContentRecommendationRepository;
import com.pulsegpt.recommendation.RecommendationStatus;
import com.pulsegpt.topic.Topic;
import com.pulsegpt.topic.TopicRepository;
import com.pulsegpt.user.User;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class CalendarService {

    private final CalendarItemRepository calendarItemRepository;
    private final ContentRecommendationRepository recommendationRepository;
    private final TopicRepository topicRepository;
    private final AuditService auditService;
    private final CalendarMapper calendarMapper;

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "scheduledStart", "scheduledEnd", "scheduledAt", "createdAt", "updatedAt", "title", "status", "priority"
    );

    private static final List<CalendarItemStatus> ACTIVE_STATUSES = List.of(
            CalendarItemStatus.SCHEDULED, CalendarItemStatus.PLANNED
    );

    @Transactional
    public CalendarItemResponse createCalendarItem(User user, CreateCalendarItemRequest request) {
        log.info("CREATING_CALENDAR_ITEM: user={} title={} platform={} start={}",
                user.getId(), request.title(), request.platform(), request.scheduledStart());

        // 1. Timezone Validation
        ZoneId zoneId = validateTimezone(request.timezone());

        // 2. Start / End Validation
        Instant startTime = request.scheduledStart();
        Instant endTime = request.scheduledEnd() != null
                ? request.scheduledEnd()
                : startTime.plus(Duration.ofHours(1));

        if (!endTime.isAfter(startTime)) {
            throw new ApiException("Scheduled end time must be strictly after scheduled start time",
                    HttpStatus.BAD_REQUEST, "INVALID_TIME_RANGE");
        }

        // 3. Past Date Validation (allow 2-minute clock skew buffer)
        if (startTime.isBefore(Instant.now().minus(Duration.ofMinutes(2)))) {
            throw new ApiException("Cannot schedule content in the past",
                    HttpStatus.BAD_REQUEST, "PAST_DATE_NOT_ALLOWED");
        }

        // 4. Recommendation Provenance & Approval Check
        ContentRecommendation recommendation = null;
        if (request.recommendationId() != null) {
            recommendation = recommendationRepository.findByIdAndUserId(request.recommendationId(), user.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Recommendation not found with id: " + request.recommendationId()));

            if (recommendation.getStatus() != RecommendationStatus.APPROVED) {
                throw new ApiException(
                        "Only APPROVED recommendations can be scheduled on the calendar. Current status: " + recommendation.getStatus(),
                        HttpStatus.BAD_REQUEST,
                        "UNAPPROVED_RECOMMENDATION"
                );
            }

            if (calendarItemRepository.existsByUserIdAndRecommendationIdAndStatusIn(user.getId(), recommendation.getId(), ACTIVE_STATUSES)) {
                throw new ApiException(
                        "This recommendation is already scheduled on your calendar",
                        HttpStatus.BAD_REQUEST,
                        "DUPLICATE_RECOMMENDATION_SCHEDULE"
                );
            }
        }

        // 5. Topic Association
        Topic topic = null;
        if (request.topicId() != null) {
            topic = topicRepository.findByIdAndUserId(request.topicId(), user.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Topic not found with id: " + request.topicId()));
        } else if (recommendation != null && recommendation.getTopic() != null) {
            topic = recommendation.getTopic();
        }

        // 6. Deterministic Conflict Detection
        List<CalendarItem> conflicts = calendarItemRepository.findOverlappingItems(
                user.getId(), request.platform(), startTime, endTime, ACTIVE_STATUSES, null
        );

        if (!conflicts.isEmpty()) {
            List<CalendarConflictDetail> conflictDetails = conflicts.stream()
                    .map(c -> CalendarConflictDetail.builder()
                            .calendarItemId(c.getId())
                            .title(c.getTitle())
                            .platform(c.getPlatform())
                            .scheduledStart(c.getScheduledStart())
                            .scheduledEnd(c.getScheduledEnd())
                            .build())
                    .toList();

            auditService.logAuditEvent(user, "CALENDAR_CONFLICT_DETECTED",
                    conflicts.get(0).getId().toString(),
                    Map.of("platform", request.platform().name(),
                            "start", startTime.toString(),
                            "end", endTime.toString(),
                            "conflictingCount", conflicts.size()));

            log.warn("CALENDAR_CONFLICT: user={} platform={} conflictingCount={}",
                    user.getId(), request.platform(), conflicts.size());

            throw new CalendarConflictException("Scheduling conflict detected with existing items on " + request.platform(), conflictDetails);
        }

        // 7. Topic Diversity / Recency Warning
        List<TopicRecencyWarning> warnings = new ArrayList<>();
        if (topic != null) {
            Instant since = startTime.minus(Duration.ofDays(7));
            Instant until = startTime.plus(Duration.ofDays(7));
            List<CalendarItem> recentItems = calendarItemRepository.findRecentItemsByTopic(
                    user.getId(), topic.getId(), since, until, ACTIVE_STATUSES, null
            );
            if (!recentItems.isEmpty()) {
                warnings.add(TopicRecencyWarning.builder()
                        .type("TOPIC_RECENCY_WARNING")
                        .message("Topic '" + topic.getName() + "' was scheduled recently (on " + recentItems.get(0).getScheduledStart() + "). Consider diversifying topics.")
                        .severity("WARNING")
                        .topicId(topic.getId())
                        .lastScheduledDate(recentItems.get(0).getScheduledStart())
                        .build());
            }
        }

        // 8. Build and Save Calendar Item
        String contentType = request.contentType();
        if (contentType == null && recommendation != null) {
            contentType = recommendation.getContentType();
        }
        if (contentType == null) {
            contentType = "VIDEO";
        }

        Priority priority = request.priority();
        if (priority == null && recommendation != null) {
            priority = recommendation.getPriority();
        }
        if (priority == null) {
            priority = Priority.MEDIUM;
        }

        CalendarItem item = CalendarItem.builder()
                .user(user)
                .recommendation(recommendation)
                .topic(topic)
                .title(request.title())
                .description(request.notes())
                .notes(request.notes())
                .contentType(contentType)
                .platform(request.platform())
                .scheduledStart(startTime)
                .scheduledEnd(endTime)
                .scheduledAt(startTime)
                .timezone(zoneId.getId())
                .status(CalendarItemStatus.SCHEDULED)
                .priority(priority)
                .approvedAt(recommendation != null ? recommendation.getApprovedAt() : Instant.now())
                .build();

        item = calendarItemRepository.save(item);

        auditService.logAuditEvent(user, "CALENDAR_ITEM_CREATED", item.getId().toString(),
                Map.of(
                        "calendarItemId", item.getId().toString(),
                        "title", item.getTitle(),
                        "platform", item.getPlatform().name(),
                        "scheduledStart", item.getScheduledStart().toString(),
                        "recommendationId", recommendation != null ? recommendation.getId().toString() : "NONE",
                        "topicId", topic != null ? topic.getId().toString() : "NONE"
                ));

        CalendarItemResponse response = calendarMapper.toResponse(item);
        if (!warnings.isEmpty()) {
            response = CalendarItemResponse.builder()
                    .id(response.id())
                    .userId(response.userId())
                    .recommendationId(response.recommendationId())
                    .topicId(response.topicId())
                    .topicName(response.topicName())
                    .title(response.title())
                    .description(response.description())
                    .notes(response.notes())
                    .contentType(response.contentType())
                    .platform(response.platform())
                    .scheduledStart(response.scheduledStart())
                    .scheduledEnd(response.scheduledEnd())
                    .timezone(response.timezone())
                    .status(response.status())
                    .priority(response.priority())
                    .approvedAt(response.approvedAt())
                    .cancelledAt(response.cancelledAt())
                    .createdAt(response.createdAt())
                    .updatedAt(response.updatedAt())
                    .evidenceSnapshot(response.evidenceSnapshot())
                    .validationPassed(response.validationPassed())
                    .warnings(warnings)
                    .build();
        }

        return response;
    }

    @Transactional
    public CalendarItemResponse updateCalendarItem(User user, UUID id, UpdateCalendarItemRequest request) {
        CalendarItem item = calendarItemRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Calendar item not found with id: " + id));

        if (item.getStatus() == CalendarItemStatus.CANCELLED) {
            throw new ApiException("Cannot modify a cancelled calendar item", HttpStatus.BAD_REQUEST, "ITEM_ALREADY_CANCELLED");
        }

        ZoneId zoneId = request.timezone() != null ? validateTimezone(request.timezone()) : ZoneId.of(item.getTimezone());

        Instant newStart = request.scheduledStart() != null ? request.scheduledStart() : item.getScheduledStart();
        Instant newEnd = request.scheduledEnd() != null ? request.scheduledEnd() : item.getScheduledEnd();

        if (request.scheduledStart() != null && request.scheduledEnd() == null) {
            Duration existingDuration = Duration.between(item.getScheduledStart(), item.getScheduledEnd());
            if (existingDuration.isZero() || existingDuration.isNegative()) {
                existingDuration = Duration.ofHours(1);
            }
            newEnd = newStart.plus(existingDuration);
        }

        if (!newEnd.isAfter(newStart)) {
            throw new ApiException("Scheduled end time must be strictly after scheduled start time",
                    HttpStatus.BAD_REQUEST, "INVALID_TIME_RANGE");
        }

        // If time changed, validate not in past
        if (request.scheduledStart() != null && !request.scheduledStart().equals(item.getScheduledStart())) {
            if (newStart.isBefore(Instant.now().minus(Duration.ofMinutes(2)))) {
                throw new ApiException("Cannot reschedule content to a past date",
                        HttpStatus.BAD_REQUEST, "PAST_DATE_NOT_ALLOWED");
            }
        }

        PlatformType newPlatform = request.platform() != null ? request.platform() : item.getPlatform();

        // Conflict check on update (excluding current item)
        List<CalendarItem> conflicts = calendarItemRepository.findOverlappingItems(
                user.getId(), newPlatform, newStart, newEnd, ACTIVE_STATUSES, item.getId()
        );

        if (!conflicts.isEmpty()) {
            List<CalendarConflictDetail> conflictDetails = conflicts.stream()
                    .map(c -> CalendarConflictDetail.builder()
                            .calendarItemId(c.getId())
                            .title(c.getTitle())
                            .platform(c.getPlatform())
                            .scheduledStart(c.getScheduledStart())
                            .scheduledEnd(c.getScheduledEnd())
                            .build())
                    .toList();

            auditService.logAuditEvent(user, "CALENDAR_CONFLICT_DETECTED", item.getId().toString(),
                    Map.of("platform", newPlatform.name(), "start", newStart.toString(), "end", newEnd.toString()));

            throw new CalendarConflictException("Rescheduling conflict detected on " + newPlatform, conflictDetails);
        }

        if (request.title() != null && !request.title().isBlank()) {
            item.setTitle(request.title());
        }
        if (request.contentType() != null) {
            item.setContentType(request.contentType());
        }
        if (request.platform() != null) {
            item.setPlatform(request.platform());
        }
        if (request.priority() != null) {
            item.setPriority(request.priority());
        }
        if (request.notes() != null) {
            item.setNotes(request.notes());
            item.setDescription(request.notes());
        }
        if (request.status() != null) {
            item.setStatus(request.status());
        }
        item.setScheduledStart(newStart);
        item.setScheduledEnd(newEnd);
        item.setScheduledAt(newStart);
        item.setTimezone(zoneId.getId());

        item = calendarItemRepository.save(item);

        auditService.logAuditEvent(user, "CALENDAR_ITEM_UPDATED", item.getId().toString(),
                Map.of(
                        "calendarItemId", item.getId().toString(),
                        "title", item.getTitle(),
                        "platform", item.getPlatform().name(),
                        "scheduledStart", item.getScheduledStart().toString()
                ));

        return calendarMapper.toResponse(item);
    }

    @Transactional
    public CalendarItemResponse cancelCalendarItem(User user, UUID id, String reason) {
        CalendarItem item = calendarItemRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Calendar item not found with id: " + id));

        if (item.getStatus() == CalendarItemStatus.CANCELLED) {
            return calendarMapper.toResponse(item);
        }

        item.setStatus(CalendarItemStatus.CANCELLED);
        item.setCancelledAt(Instant.now());
        item = calendarItemRepository.save(item);

        auditService.logAuditEvent(user, "CALENDAR_ITEM_CANCELLED", item.getId().toString(),
                Map.of(
                        "calendarItemId", item.getId().toString(),
                        "reason", reason != null ? reason : "CREATOR_CANCELLED",
                        "cancelledAt", item.getCancelledAt().toString()
                ));

        log.info("CALENDAR_ITEM_CANCELLED: user={} itemId={}", user.getId(), item.getId());
        return calendarMapper.toResponse(item);
    }

    @Transactional
    public void deleteCalendarItem(User user, UUID id) {
        CalendarItem item = calendarItemRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Calendar item not found with id: " + id));

        // Prefer soft cancellation for scheduled items to maintain research/audit provenance
        if (item.getStatus() == CalendarItemStatus.SCHEDULED || item.getStatus() == CalendarItemStatus.PLANNED) {
            cancelCalendarItem(user, id, "DELETED_BY_CREATOR");
        } else {
            calendarItemRepository.delete(item);
            auditService.logAuditEvent(user, "CALENDAR_ITEM_DELETED", id.toString(),
                    Map.of("calendarItemId", id.toString()));
        }
    }

    @Transactional(readOnly = true)
    public PageResponse<CalendarItemResponse> getCalendarItems(User user, CalendarQuery query) {
        Pageable pageable = PaginationUtils.createPageRequest(
                query.getPageOrDefault(),
                query.getSizeOrDefault(),
                query.sort(),
                query.direction(),
                "scheduledStart",
                ALLOWED_SORT_FIELDS
        );

        Specification<CalendarItem> spec = buildCalendarSpecification(query, user.getId());
        Page<CalendarItem> page = calendarItemRepository.findAll(spec, pageable);
        List<CalendarItemResponse> responses = calendarMapper.toResponseList(page.getContent());

        return PageResponse.from(page, responses);
    }

    @Transactional(readOnly = true)
    public CalendarItemResponse getCalendarItemById(User user, UUID id) {
        CalendarItem item = calendarItemRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Calendar item not found with id: " + id));
        return calendarMapper.toResponse(item);
    }

    @Transactional(readOnly = true)
    public CalendarConflictResponse checkConflicts(User user, PlatformType platform, Instant start, Instant end, UUID excludeId) {
        if (start == null || end == null || platform == null) {
            throw new ApiException("Platform, start time, and end time are required to check conflicts", HttpStatus.BAD_REQUEST, "INVALID_ARGUMENT");
        }

        List<CalendarItem> conflicts = calendarItemRepository.findOverlappingItems(
                user.getId(), platform, start, end, ACTIVE_STATUSES, excludeId
        );

        List<CalendarConflictDetail> details = conflicts.stream()
                .map(c -> CalendarConflictDetail.builder()
                        .calendarItemId(c.getId())
                        .title(c.getTitle())
                        .platform(c.getPlatform())
                        .scheduledStart(c.getScheduledStart())
                        .scheduledEnd(c.getScheduledEnd())
                        .build())
                .toList();

        return CalendarConflictResponse.builder()
                .conflict(!details.isEmpty())
                .message(details.isEmpty() ? "No scheduling conflicts detected" : "Detected " + details.size() + " scheduling conflict(s)")
                .conflicts(details)
                .build();
    }

    @Transactional(readOnly = true)
    public CalendarSuggestionResponse getSuggestions(User user, LocalDate date, PlatformType platform, String timezoneStr) {
        ZoneId zone = validateTimezone(timezoneStr != null ? timezoneStr : "UTC");
        LocalDate targetDate = date != null ? date : LocalDate.now(zone).plusDays(1);
        PlatformType targetPlatform = platform != null ? platform : PlatformType.YOUTUBE;

        // Deterministic windows: Morning (09:00), Afternoon (13:00), Evening (18:00)
        List<LocalTime> windowTimes = List.of(
                LocalTime.of(9, 0),
                LocalTime.of(13, 0),
                LocalTime.of(18, 0)
        );
        List<String> windowNames = List.of("Morning", "Afternoon", "Evening");

        List<CalendarSuggestionResponse.SuggestedSlot> slots = new ArrayList<>();

        for (int i = 0; i < windowTimes.size(); i++) {
            LocalTime time = windowTimes.get(i);
            String name = windowNames.get(i);

            ZonedDateTime startZdt = ZonedDateTime.of(targetDate, time, zone);
            ZonedDateTime endZdt = startZdt.plusHours(1);

            Instant startInstant = startZdt.toInstant();
            Instant endInstant = endZdt.toInstant();

            List<CalendarItem> conflicts = calendarItemRepository.findOverlappingItems(
                    user.getId(), targetPlatform, startInstant, endInstant, ACTIVE_STATUSES, null
            );

            boolean available = conflicts.isEmpty();
            String conflictReason = available ? null : "Occupied by '" + conflicts.get(0).getTitle() + "'";

            slots.add(CalendarSuggestionResponse.SuggestedSlot.builder()
                    .slotName(name)
                    .time(time.toString())
                    .scheduledStart(startInstant)
                    .scheduledEnd(endInstant)
                    .available(available)
                    .conflictReason(conflictReason)
                    .build());
        }

        return CalendarSuggestionResponse.builder()
                .date(targetDate)
                .platform(targetPlatform)
                .timezone(zone.getId())
                .slots(slots)
                .build();
    }

    private Specification<CalendarItem> buildCalendarSpecification(CalendarQuery query, UUID userId) {
        return (root, criteriaQuery, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // 1. Mandatory User Scoping
            predicates.add(cb.equal(root.get("user").get("id"), userId));

            // 2. Date Range
            if (query.startDate() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("scheduledStart"), query.startDate()));
            }
            if (query.endDate() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("scheduledStart"), query.endDate()));
            }

            // 3. Platform Filter
            if (query.platform() != null) {
                predicates.add(cb.equal(root.get("platform"), query.platform()));
            }

            // 4. Content Type Filter
            if (query.contentType() != null && !query.contentType().isBlank()) {
                predicates.add(cb.equal(root.get("contentType"), query.contentType()));
            }

            // 5. Status Filter
            if (query.status() != null) {
                predicates.add(cb.equal(root.get("status"), query.status()));
            }

            // 6. Topic Filter
            if (query.topicId() != null) {
                predicates.add(cb.equal(root.get("topic").get("id"), query.topicId()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private ZoneId validateTimezone(String timezoneStr) {
        if (timezoneStr == null || timezoneStr.isBlank()) {
            throw new ApiException("Timezone identifier cannot be blank", HttpStatus.BAD_REQUEST, "INVALID_TIMEZONE");
        }
        try {
            return ZoneId.of(timezoneStr);
        } catch (Exception ex) {
            throw new ApiException("Invalid IANA timezone identifier: " + timezoneStr,
                    HttpStatus.BAD_REQUEST, "INVALID_TIMEZONE");
        }
    }
}
