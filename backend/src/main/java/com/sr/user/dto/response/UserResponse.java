package com.sr.user.dto.response;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record UserResponse(
    UUID id,
    String displayName,
    String email,
    String role,
    boolean active,
    OffsetDateTime createdAt,
    List<UUID> classIds
) {}
