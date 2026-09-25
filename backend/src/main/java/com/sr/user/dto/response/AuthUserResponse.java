package com.sr.user.dto.response;

import java.util.UUID;

public record AuthUserResponse(
    UUID id,
    String username,
    String displayName,
    String role
) {
}
