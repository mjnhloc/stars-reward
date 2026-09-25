package com.sr.auth.dto.response;

import com.sr.user.dto.response.AuthUserResponse;

public record LoginResponse(
    String accessToken,
    String tokenType,
    long expiresIn,
    AuthUserResponse user
) {
}
