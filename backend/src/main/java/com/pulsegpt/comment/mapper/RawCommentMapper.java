package com.pulsegpt.comment.mapper;

import com.pulsegpt.comment.RawComment;
import com.pulsegpt.comment.dto.RawCommentResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface RawCommentMapper {

    @Mapping(target = "postId", source = "post.id")
    @Mapping(target = "postTitle", source = "post.title")
    @Mapping(target = "platform", source = "post.platformAccount.platform")
    RawCommentResponse toResponse(RawComment rawComment);

    List<RawCommentResponse> toResponseList(List<RawComment> rawComments);
}
