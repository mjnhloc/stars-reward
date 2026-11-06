package com.sr.scoreimport.dto.response;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/*
 * status: COMPLETED or REVERTED
 */
public record ImportBatchResponse(
    UUID id,
    String fileName,
    String status,
    OffsetDateTime importedAt,
    String importedBy,
    long sessionCount,
    long studentCount,
    long awardedStars,
    List<String> classNames,
    OffsetDateTime revertedAt,
    String revertedBy
) {}
