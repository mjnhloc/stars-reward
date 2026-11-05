package com.sr.scoreimport.dto.response;

import java.util.UUID;

public record ScoreImportResultResponse(
    UUID batchId,
    int importedSessions,
    int awardedStars,
    int students,
    int skippedSessions
) {}
