package com.sr.student.dto.response;

import java.util.UUID;

public record StudentSummary(
        UUID id,
        String studentCode,
        String fullName,
        UUID classId,
        String className,
        int totalStars
) {}