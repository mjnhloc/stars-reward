package com.sr.student.dto.request;

import jakarta.validation.constraints.*;
import java.util.UUID;

public record StudentStarsAdjustmentRequest(
    @NotNull UUID classId,
    @NotNull @Min(-1000) @Max(1000) Integer stars,
    @NotBlank @Size(max = 500) String reasonDetail
) {}
