package com.sr.auth.dto.response;

import java.util.UUID;

public record AuthenticatedUserResponse(
    UUID id,
    String email,
    String passwordHash,
    String displayName,
    String role,
    boolean active
) {
}
