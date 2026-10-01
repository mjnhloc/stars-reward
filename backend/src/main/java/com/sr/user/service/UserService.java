package com.sr.user.service;

import java.util.UUID;

import com.sr.user.dto.response.AuthUserResponse;

public interface UserService {

    UUID getUserId();

    AuthUserResponse getCurrentUser(UUID userId);
}
