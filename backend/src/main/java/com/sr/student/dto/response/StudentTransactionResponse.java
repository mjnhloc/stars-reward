package com.sr.student.dto.response;

import java.time.OffsetDateTime;
import java.util.UUID;

public record StudentTransactionResponse(
    UUID id,
    String transactionType,
    String sourceType,
    Integer starDelta,
    Integer balanceBefore,
    Integer balanceAfter,
    String reasonDetail,
    OffsetDateTime createdAt,
    String displayName,
    String className
) {}
