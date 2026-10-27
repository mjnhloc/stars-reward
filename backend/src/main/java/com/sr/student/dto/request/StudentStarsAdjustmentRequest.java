package com.sr.student.dto.request;

import java.util.UUID;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record StudentStarsAdjustmentRequest(
    @NotNull UUID classId,
    @NotNull @Min(-1000) @Max(1000) Integer stars,
    @NotBlank @Size(max = 500) String reasonDetail
) {}
