package com.pulsegpt.topic.mapper;

import com.pulsegpt.topic.Topic;
import com.pulsegpt.topic.dto.TopicResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface TopicMapper {

    @Mapping(source = "lastClusteringRun.id", target = "lastClusteringRunId")
    TopicResponse toResponse(Topic topic);

    List<TopicResponse> toResponseList(List<Topic> topics);
}
