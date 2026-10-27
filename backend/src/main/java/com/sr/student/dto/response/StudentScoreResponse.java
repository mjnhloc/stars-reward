package com.sr.student.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public record StudentScoreResponse(
    UUID id,
    BigDecimal homeworkScore,
    BigDecimal testScore,
    BigDecimal finalScore,
    int awardedStars,
    LocalDate assessmentDate,
    OffsetDateTime createdAt
) {}
