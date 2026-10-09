package com.pulsegpt.comment.mapper;

import com.pulsegpt.comment.Post;
import com.pulsegpt.comment.dto.PostResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface PostMapper {

    @Mapping(target = "platformAccountId", source = "platformAccount.id")
    @Mapping(target = "platform", source = "platformAccount.platform")
    @Mapping(target = "accountName", source = "platformAccount.accountName")
    PostResponse toResponse(Post post);

    List<PostResponse> toResponseList(List<Post> posts);
}
