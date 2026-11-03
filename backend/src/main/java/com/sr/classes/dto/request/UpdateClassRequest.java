package com.sr.classes.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateClassRequest(
    @NotBlank(message = "Mã lớp không được để trống") @Size(max = 50, message = "Mã lớp không được vượt quá 50 kí tự") String code,
    @NotBlank(message = "Tên lớp không được để trống") @Size(max = 150, message = "Tên lớp không được vượt quá 150 kí tự") String name,
    @NotBlank(message = "Năm bắt đầu không được để trống") String schoolYear
) {}
