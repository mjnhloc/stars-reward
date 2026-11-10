package com.sr.student.dto.request;

import java.time.LocalDate;
import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateStudentRequest(

    @NotBlank(message = "Mã học sinh không được để trống") @Size(max = 50, message = "Mã học sinh không được vượt quá 50 ký tự") String studentCode,

    @NotBlank(message = "Tên học sinh không được để trống") @Size(max = 200, message = "Tên học sinh không được vượt quá 200 ký tự") String fullName,

    @NotNull(message = "Lớp học không được để trống") UUID classId,

    @NotNull(message = "Ngày bắt đầu học không được để trống") LocalDate startedAt
) {}
