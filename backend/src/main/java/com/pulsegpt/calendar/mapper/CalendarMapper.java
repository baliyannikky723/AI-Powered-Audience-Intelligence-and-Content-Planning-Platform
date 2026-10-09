package com.pulsegpt.calendar.mapper;

import com.pulsegpt.calendar.CalendarItem;
import com.pulsegpt.calendar.dto.CalendarItemResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface CalendarMapper {

    @Mapping(source = "user.id", target = "userId")
    @Mapping(source = "topic.id", target = "topicId")
    @Mapping(source = "topic.name", target = "topicName")
    @Mapping(source = "recommendation.id", target = "recommendationId")
    @Mapping(source = "recommendation.evidenceSnapshot", target = "evidenceSnapshot")
    @Mapping(source = "recommendation.validationPassed", target = "validationPassed")
    @Mapping(target = "warnings", ignore = true)
    CalendarItemResponse toResponse(CalendarItem item);

    List<CalendarItemResponse> toResponseList(List<CalendarItem> items);
}
