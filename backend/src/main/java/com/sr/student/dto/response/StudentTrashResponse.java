package com.sr.student.dto.response;

import java.time.OffsetDateTime;
import java.util.UUID;

public record StudentTrashResponse(
    UUID id,
    String studentCode,
    String fullName,
    UUID lastClassId,
    String lastClassName,
    boolean lastClassArchived,
    OffsetDateTime archivedAt,
    int totalStars
) {}
