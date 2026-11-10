package com.sr.auth.dto.response;

public record TokenResponse(
    String accessToken,
    String tokenType,
    long expiresIn,
    boolean remember
) {}
