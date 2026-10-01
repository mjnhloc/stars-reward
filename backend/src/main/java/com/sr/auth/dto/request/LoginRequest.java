package com.sr.auth.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
    @NotBlank(message = "Email không được để trống") @Email(message = "Email không hợp lệ") @Size(max = 250) String email,

    @NotBlank(message = "Mật khẩu không được để trống") @Size(max = 100) String password,

    boolean remember
) {
}
