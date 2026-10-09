package com.pulsegpt.platform.mapper;

import com.pulsegpt.platform.PlatformAccount;
import com.pulsegpt.platform.dto.PlatformAccountResponse;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface PlatformAccountMapper {

    PlatformAccountResponse toResponse(PlatformAccount account);

    List<PlatformAccountResponse> toResponseList(List<PlatformAccount> accounts);
}
