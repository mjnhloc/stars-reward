package com.sr.auth.dto;

public record AccessToken(
    String value,
    long expiresIn
) {}
