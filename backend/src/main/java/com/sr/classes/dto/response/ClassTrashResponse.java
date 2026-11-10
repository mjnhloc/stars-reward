package com.sr.classes.dto.response;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ClassTrashResponse(
    UUID id,
    String code,
    String name,
    String schoolYear,
    OffsetDateTime archivedAt,
    int studentCount
) {}
