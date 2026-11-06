package com.sr.scoreimport.dto.response;

import java.util.List;
import java.util.UUID;

public record ImportRevertPreviewResponse(
    UUID batchId,
    String fileName,
    int sessionCount,
    int starsToReverse,
    List<ImportRevertStudentResponse> students,
    boolean canRevert
) {}
