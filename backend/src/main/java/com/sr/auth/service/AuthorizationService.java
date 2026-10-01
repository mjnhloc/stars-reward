package com.sr.auth.service;

import java.util.UUID;

import com.sr.auth.dto.request.LoginRequest;
import com.sr.auth.dto.response.LoginResponse;

public interface AuthorizationService {

    void requireView(UUID user, UUID classId);

    void requireAdjust(UUID user, UUID classId);

    LoginResponse login(LoginRequest loginRequest);
}
