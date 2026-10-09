package com.pulsegpt.auth.service;

import com.pulsegpt.config.AppProperties;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CookieService {

    private final AppProperties appProperties;

    public ResponseCookie createRefreshTokenCookie(String rawRefreshToken) {
        AppProperties.Cookie cookieProps = appProperties.getCookie();

        return ResponseCookie.from(cookieProps.getName(), rawRefreshToken)
                .httpOnly(cookieProps.isHttpOnly())
                .secure(cookieProps.isSecure())
                .sameSite(cookieProps.getSameSite())
                .path(cookieProps.getPath())
                .maxAge(cookieProps.getMaxAgeSeconds())
                .build();
    }

    public ResponseCookie createClearRefreshTokenCookie() {
        AppProperties.Cookie cookieProps = appProperties.getCookie();

        return ResponseCookie.from(cookieProps.getName(), "")
                .httpOnly(cookieProps.isHttpOnly())
                .secure(cookieProps.isSecure())
                .sameSite(cookieProps.getSameSite())
                .path(cookieProps.getPath())
                .maxAge(0)
                .build();
    }

    public Optional<String> extractRefreshTokenFromCookie(HttpServletRequest request) {
        if (request.getCookies() == null) {
            return Optional.empty();
        }

        String cookieName = appProperties.getCookie().getName();
        return Arrays.stream(request.getCookies())
                .filter(c -> cookieName.equals(c.getName()))
                .map(Cookie::getValue)
                .filter(v -> v != null && !v.isBlank())
                .findFirst();
    }
}
