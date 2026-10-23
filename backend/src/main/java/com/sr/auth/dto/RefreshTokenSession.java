package com.sr.auth.dto;

import java.time.Instant;
import java.util.UUID;

public record RefreshTokenSession(
    UUID sessionId,
    UUID familyId,
    UUID userId,
    String email,
    String role,
    boolean remember,
    Instant issuedAt,
    Instant expiresAt
) {}
