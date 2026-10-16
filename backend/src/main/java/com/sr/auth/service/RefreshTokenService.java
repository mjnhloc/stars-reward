package com.sr.auth.service;

import java.util.UUID;

import com.sr.auth.dto.CreatedRefreshToken;

import jakarta.servlet.http.HttpServletResponse;

public interface RefreshTokenService {
    CreatedRefreshToken create(UUID userId, String email, String role);

    CreatedRefreshToken rotate(String refreshToken);

    void revoke(String refreshToken);

    void revokeFamily(UUID familyId);

    void writeToCookie(HttpServletResponse response, String refreshToken);

    void clearCookie(HttpServletResponse response);
}
