package com.pulsegpt.security;

import com.pulsegpt.common.exception.UnauthorizedException;
import com.pulsegpt.user.User;
import com.pulsegpt.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CurrentUserService {

    private final UserRepository userRepository;

    public Optional<UUID> getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            return Optional.empty();
        }

        if (authentication.getPrincipal() instanceof UserPrincipal principal) {
            return Optional.of(principal.getId());
        }

        return Optional.empty();
    }

    @Transactional(readOnly = true)
    public Optional<User> getCurrentUser() {
        return getCurrentUserId().flatMap(userRepository::findById);
    }

    public UUID requireUserId() {
        return getCurrentUserId()
                .orElseThrow(() -> new UnauthorizedException("User is not authenticated"));
    }

    @Transactional(readOnly = true)
    public User requireUser() {
        UUID userId = requireUserId();
        return userRepository.findById(userId)
                .orElseThrow(() -> new UnauthorizedException("Authenticated user not found in database"));
    }
}
