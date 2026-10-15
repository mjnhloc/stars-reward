package com.sr.exception.dto;

import java.time.OffsetDateTime;

public record ApiErrorResponse(
    int status,
    String code,
    String message,
    String path,
    OffsetDateTime timestamp
) {
}
