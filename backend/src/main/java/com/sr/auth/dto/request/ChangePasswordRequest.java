package com.sr.auth.dto.request;

import jakarta.validation.constraints.NotBlank;

// @formatter:off
public record ChangePasswordRequest(
    @NotBlank(message = "Mật khẩu hiện tại không được để trống") 
    String currentPassword,

    @NotBlank(message = "Mật khẩu mới không được để trống") 
    String newPassword,

    @NotBlank(message = "Xác nhận mật khẩu mới không được để trống")
    String confirmPassword
) {}
