package com.pulsegpt.comment.mapper;

import com.pulsegpt.comment.ProcessedComment;
import com.pulsegpt.comment.dto.ProcessedCommentResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ProcessedCommentMapper {

    @Mapping(target = "rawCommentId", source = "rawComment.id")
    ProcessedCommentResponse toResponse(ProcessedComment processedComment);

    List<ProcessedCommentResponse> toResponseList(List<ProcessedComment> processedComments);
}
