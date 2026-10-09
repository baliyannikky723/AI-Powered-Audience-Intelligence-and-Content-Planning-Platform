package com.pulsegpt.calendar.controller;

import com.pulsegpt.calendar.dto.*;
import com.pulsegpt.calendar.exception.CalendarConflictException;
import com.pulsegpt.calendar.service.CalendarService;
import com.pulsegpt.common.ApiResponse;
import com.pulsegpt.common.dto.PageResponse;
import com.pulsegpt.platform.PlatformType;
import com.pulsegpt.security.CurrentUserService;
import com.pulsegpt.user.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/calendar")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Content Calendar", description = "Endpoints for creator-controlled content planning, deterministic conflict detection, and scheduling")
public class CalendarController {

    private final CalendarService calendarService;
    private final CurrentUserService currentUserService;

    @PostMapping
    @Operation(summary = "Schedule a planned content piece or approved recommendation into the editorial calendar")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Calendar item scheduled successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid schedule parameters or unapproved recommendation"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Scheduling conflict detected on target platform")
    })
    public ResponseEntity<ApiResponse<CalendarItemResponse>> createCalendarItem(
            @Valid @RequestBody CreateCalendarItemRequest request
    ) {
        User currentUser = currentUserService.requireUser();
        CalendarItemResponse response = calendarService.createCalendarItem(currentUser, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Content scheduled to calendar successfully", response));
    }

    @GetMapping
    @Operation(summary = "Get paginated calendar items with date range, platform, status, and topic filters")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Calendar items retrieved"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid query parameters"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<ApiResponse<PageResponse<CalendarItemResponse>>> getCalendarItems(
            @Valid @ModelAttribute CalendarQuery query
    ) {
        User currentUser = currentUserService.requireUser();
        PageResponse<CalendarItemResponse> response = calendarService.getCalendarItems(currentUser, query);
        return ResponseEntity.ok(ApiResponse.ok("Calendar items retrieved successfully", response));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get single calendar item by ID with full recommendation and evidence traceability")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Calendar item retrieved"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Calendar item not found")
    })
    public ResponseEntity<ApiResponse<CalendarItemResponse>> getCalendarItemById(@PathVariable UUID id) {
        User currentUser = currentUserService.requireUser();
        CalendarItemResponse response = calendarService.getCalendarItemById(currentUser, id);
        return ResponseEntity.ok(ApiResponse.ok("Calendar item retrieved successfully", response));
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Update or reschedule a calendar item with conflict detection re-validation")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Calendar item updated successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid update parameters"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Calendar item not found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Rescheduling conflict detected")
    })
    public ResponseEntity<ApiResponse<CalendarItemResponse>> updateCalendarItem(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateCalendarItemRequest request
    ) {
        User currentUser = currentUserService.requireUser();
        CalendarItemResponse response = calendarService.updateCalendarItem(currentUser, id, request);
        return ResponseEntity.ok(ApiResponse.ok("Calendar item updated successfully", response));
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "Cancel a planned/scheduled calendar item while preserving research and audit provenance")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Calendar item cancelled"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Calendar item not found")
    })
    public ResponseEntity<ApiResponse<CalendarItemResponse>> cancelCalendarItem(
            @PathVariable UUID id,
            @RequestParam(required = false) String reason
    ) {
        User currentUser = currentUserService.requireUser();
        CalendarItemResponse response = calendarService.cancelCalendarItem(currentUser, id, reason);
        return ResponseEntity.ok(ApiResponse.ok("Calendar item cancelled successfully", response));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete or cancel a calendar item")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Calendar item removed"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Calendar item not found")
    })
    public ResponseEntity<ApiResponse<Void>> deleteCalendarItem(@PathVariable UUID id) {
        User currentUser = currentUserService.requireUser();
        calendarService.deleteCalendarItem(currentUser, id);
        return ResponseEntity.ok(ApiResponse.ok("Calendar item deleted successfully", null));
    }

    @GetMapping("/conflicts")
    @Operation(summary = "Check for scheduling conflicts before placing an item on the calendar")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Conflict analysis result"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid parameters"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<ApiResponse<CalendarConflictResponse>> checkConflicts(
            @RequestParam PlatformType platform,
            @RequestParam Instant start,
            @RequestParam Instant end,
            @RequestParam(required = false) UUID excludeId
    ) {
        User currentUser = currentUserService.requireUser();
        CalendarConflictResponse response = calendarService.checkConflicts(currentUser, platform, start, end, excludeId);
        return ResponseEntity.ok(ApiResponse.ok("Conflict check completed", response));
    }

    @GetMapping("/suggestions")
    @Operation(summary = "Get deterministic posting window slot suggestions for a date and platform")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Slot suggestions retrieved"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<ApiResponse<CalendarSuggestionResponse>> getSuggestions(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) PlatformType platform,
            @RequestParam(required = false, defaultValue = "UTC") String timezone
    ) {
        User currentUser = currentUserService.requireUser();
        CalendarSuggestionResponse response = calendarService.getSuggestions(currentUser, date, platform, timezone);
        return ResponseEntity.ok(ApiResponse.ok("Slot suggestions retrieved successfully", response));
    }

    @ExceptionHandler(CalendarConflictException.class)
    public ResponseEntity<CalendarConflictResponse> handleCalendarConflict(CalendarConflictException ex) {
        CalendarConflictResponse body = CalendarConflictResponse.builder()
                .conflict(true)
                .message(ex.getMessage())
                .conflicts(ex.getConflicts())
                .build();
        return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
    }
}
