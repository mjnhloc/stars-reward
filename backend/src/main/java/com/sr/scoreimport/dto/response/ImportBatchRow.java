package com.sr.scoreimport.dto.response;

import java.time.OffsetDateTime;
import java.util.UUID;

/*
 * Import batch with the names of the users who imported and reverted it
 */
public record ImportBatchRow(
    UUID id,
    String fileName,
    String status,
    OffsetDateTime importedAt,
    String importedBy,
    OffsetDateTime revertedAt,
    String revertedBy
) {}
