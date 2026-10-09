package com.pulsegpt.topic.mapper;

import com.pulsegpt.topic.ClusteringRun;
import com.pulsegpt.topic.dto.ClusteringRunResponse;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ClusteringRunMapper {

    ClusteringRunResponse toResponse(ClusteringRun run);

    List<ClusteringRunResponse> toResponseList(List<ClusteringRun> runs);
}
