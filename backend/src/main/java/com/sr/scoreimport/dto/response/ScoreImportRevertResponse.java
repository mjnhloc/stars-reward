package com.sr.scoreimport.dto.response;

import java.util.UUID;

public record ScoreImportRevertResponse(
    UUID batchId,
    int voidedSessions,
    int reversedStars,
    int students
) {}
