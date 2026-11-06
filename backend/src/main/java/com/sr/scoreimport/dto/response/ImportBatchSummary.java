package com.sr.scoreimport.dto.response;

import java.util.UUID;

public record ImportBatchSummary(
    UUID batchId,
    Long sessionCount,
    Long studentCount,
    Long awardedStars
) {}
