package com.sr.classes.dto.response;

import java.util.UUID;

public record ClassResponse(
    UUID classId,
    String classCode,
    String className,
    Integer studentCount,
    Integer totalStars
) {}
