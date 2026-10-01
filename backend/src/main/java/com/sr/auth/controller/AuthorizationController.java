package com.sr.auth.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sr.auth.dto.request.LoginRequest;
import com.sr.auth.dto.response.LoginResponse;
import com.sr.auth.service.AuthorizationService;
import com.sr.user.dto.response.AuthUserResponse;
import com.sr.user.service.UserService;

import jakarta.validation.Valid;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthorizationController {

    private final AuthorizationService authorizationService;
    private final UserService userService;

    public AuthorizationController(AuthorizationService authorizationService, UserService userService) {
        this.authorizationService = authorizationService;
        this.userService = userService;
    }

    @GetMapping("/me")
    public AuthUserResponse getCurrentUser(@AuthenticationPrincipal Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getSubject());

        return userService.getCurrentUser(userId);
    }

    @PostMapping("/login")
    public LoginResponse login(
        @RequestBody 
        @Valid 
        LoginRequest loginRequest
    ) {
        return authorizationService.login(loginRequest);
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public String logout(@RequestBody String entity) {
        // TODO: process POST request

        return entity;
    }

}
