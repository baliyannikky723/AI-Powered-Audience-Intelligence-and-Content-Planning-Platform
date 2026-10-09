package com.pulsegpt.auth.mapper;

import com.pulsegpt.user.User;
import com.pulsegpt.user.dto.UserResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface AuthMapper {

    UserResponse toUserResponse(User user);
}
