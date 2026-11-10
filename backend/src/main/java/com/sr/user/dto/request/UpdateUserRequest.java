package com.sr.user.dto.request;

import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateUserRequest(

    @NotBlank(message = "Tên hiển thị không được để trống") @Size(max = 150, message = "Tên hiển thị không được vượt quá 150 ký tự") String displayName,

    @NotBlank(message = "Email không được để trống") @Email(message = "Email không hợp lệ") @Size(max = 250, message = "Email không được vượt quá 250 ký tự") String email,

    @NotBlank(message = "Vai trò không được để trống") @Pattern(regexp = "ADMIN|TEACHER|VIEWER", message = "Vai trò không hợp lệ") String role,

    @NotNull(message = "Trạng thái không được để trống") Boolean active,

    /*
     * null keeps the current password
     */
    @Size(min = 8, max = 100, message = "Mật khẩu phải có từ 8 đến 100 ký tự") String newPassword,

    List<UUID> classIds
) {}
