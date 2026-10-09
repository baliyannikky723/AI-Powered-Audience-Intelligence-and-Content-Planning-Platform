package com.pulsegpt.calendar.exception;

import com.pulsegpt.calendar.dto.CalendarConflictDetail;
import com.pulsegpt.common.exception.ApiException;
import lombok.Getter;
import org.springframework.http.HttpStatus;

import java.util.List;

@Getter
public class CalendarConflictException extends ApiException {

    private final List<CalendarConflictDetail> conflicts;

    public CalendarConflictException(String message, List<CalendarConflictDetail> conflicts) {
        super(message, HttpStatus.CONFLICT, "SCHEDULING_CONFLICT");
        this.conflicts = conflicts != null ? conflicts : List.of();
    }
}
