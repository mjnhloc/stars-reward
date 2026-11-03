package com.sr.classes.dto.response;

import java.util.UUID;

public record ClassDetailResponse(
        UUID id,
        String code,
        String name,
        String schoolYear
) {}
