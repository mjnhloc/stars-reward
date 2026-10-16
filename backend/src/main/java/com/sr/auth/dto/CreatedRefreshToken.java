package com.sr.auth.dto;

public record CreatedRefreshToken(
    String token,
    RefreshTokenSession session
) {}
