package com.pulsegpt.recommendation.mapper;

import com.pulsegpt.recommendation.ContentRecommendation;
import com.pulsegpt.recommendation.dto.RecommendationResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ContentRecommendationMapper {

    @Mapping(source = "topic.id", target = "topicId")
    @Mapping(source = "topic.name", target = "topicName")
    @Mapping(source = "problemAddress", target = "problemAddressed")
    @Mapping(source = "validationJson", target = "validation")
    @Mapping(source = "repairResultJson", target = "repairResult")
    RecommendationResponse toResponse(ContentRecommendation entity);

    List<RecommendationResponse> toResponseList(List<ContentRecommendation> entities);
}
