package com.sr.auth.service;

import java.time.Instant;
import java.util.UUID;

import com.sr.auth.dto.CreatedRefreshToken;

import jakarta.servlet.http.HttpServletResponse;

public interface RefreshTokenService {
    CreatedRefreshToken create(UUID userId, String email, String role, boolean remember);

    CreatedRefreshToken rotate(String refreshToken);

    void revoke(String refreshToken);

    void revokeFamily(UUID familyId);

    void revokeAllByUserId(UUID userId);

    void writeToCookie(HttpServletResponse response, String refreshToken, boolean remember, Instant expiresAt);

    void clearCookie(HttpServletResponse response);
}
