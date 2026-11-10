package com.sr.user.service;

import java.util.List;
import java.util.UUID;

import com.sr.user.dto.request.CreateUserRequest;
import com.sr.user.dto.request.UpdateUserRequest;
import com.sr.user.dto.response.AuthUserResponse;
import com.sr.user.dto.response.UserResponse;

public interface UserService {

    UUID getUserId();

    AuthUserResponse getCurrentUser(UUID userId);

    List<UserResponse> getUsers();

    UserResponse createUser(CreateUserRequest request);

    UserResponse updateUser(UUID userId, UpdateUserRequest request);
}
