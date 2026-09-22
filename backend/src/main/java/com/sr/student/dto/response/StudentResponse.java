package com.sr.student.dto.response;

import java.math.BigDecimal;
import java.util.UUID;

public record StudentResponse(
        UUID id,
        String studentCode,
        String fullName,
        UUID classId,
        String className,
        BigDecimal homeworkScore,
        BigDecimal testScore,
        BigDecimal finalScore,
        int totalStars,
        int redeemableStars,
        long redeemableValue,
        int remainingStars
) {}