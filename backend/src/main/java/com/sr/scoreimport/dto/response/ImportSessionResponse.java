package com.sr.scoreimport.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;

/*
 * status: NEW (will be imported), EXISTS (already imported), CONFLICT (already
 * imported with different scores, not changed)
 */
public record ImportSessionResponse(
    LocalDate assessmentDate,
    BigDecimal homeworkScore,
    BigDecimal testScore,
    BigDecimal finalScore,
    int awardedStars,
    String status
) {}
