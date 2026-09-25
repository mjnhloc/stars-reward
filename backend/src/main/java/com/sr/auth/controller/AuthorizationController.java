package com.sr.auth.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sr.auth.dto.request.LoginRequest;
import com.sr.auth.dto.response.LoginResponse;
import com.sr.auth.service.AuthorizationService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthorizationController {

    private final AuthorizationService authorizationService;

    public AuthorizationController(AuthorizationService authorizationService) {
        this.authorizationService = authorizationService;
    }

    @GetMapping("/me")
    public String getMethodName(@RequestParam String param) {
        return new String();
    }

    @PostMapping("/login")
    public LoginResponse login(@RequestBody LoginRequest loginRequest) {
        return authorizationService.login(loginRequest.username(), loginRequest.password());
    }

    @PostMapping("/logout")
    public String logout(@RequestBody String entity) {
        // TODO: process POST request

        return entity;
    }

}
