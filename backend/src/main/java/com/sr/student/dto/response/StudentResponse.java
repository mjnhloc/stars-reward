package com.sr.student.dto.response;

import java.util.UUID;

public record StudentResponse(
        UUID id,
        String studentCode,
        String fullName,
        UUID classId,
        String className,
        int totalStars,
        int redeemableStars,
        long redeemableValue,
        int remainingStars
) {}