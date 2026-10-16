package com.sr.auth.service.impl;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sr.auth.dto.CreatedRefreshToken;
import com.sr.auth.dto.RefreshTokenSession;
import com.sr.auth.service.RefreshTokenService;
import com.sr.exception.InvalidRefreshTokenException;

import jakarta.servlet.http.HttpServletResponse;

@Service
public class RefreshTokenServiceImpl implements RefreshTokenService {

    private static final String TOKEN_PREFIX = "refresh-token:";

    private static final String FAMILY_PREFIX = "refresh-family:";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final SecureRandom secureRandom;
    private final Duration expiration;
    private final String cookieName;
    private final boolean secure;
    private final String sameSite;

    public RefreshTokenServiceImpl(
        StringRedisTemplate redisTemplate,
        ObjectMapper objectMapper,
        @Value("${app.security.refresh-token.expiration-seconds}")
        long expirationSeconds,
        @Value("${app.security.refresh-token.cookie-name}")
        String cookieName,
        @Value("${app.security.refresh-token.secure:false}")
        boolean secure,
        @Value("${app.security.refresh-token.same-site}")
        String sameSite
    ) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.secureRandom = new SecureRandom();
        this.expiration = Duration.ofSeconds(expirationSeconds);
        this.cookieName = cookieName;
        this.secure = secure;
        this.sameSite = sameSite;
    }

    @Override
    public CreatedRefreshToken create(UUID userId, String email, String role) {
        UUID familyId = UUID.randomUUID();

        return createToken(userId, email, role, familyId);
    }

    @Override
    public CreatedRefreshToken rotate(String refreshToken) {
        String tokenKey = tokenKey(refreshToken);

        String sessionJson = redisTemplate
            .opsForValue()
            .get(tokenKey);

        if (sessionJson == null) {
            throw new InvalidRefreshTokenException(
                "Refresh token không hợp lệ hoặc đã hết hạn"
            );
        }

        RefreshTokenSession oldSession = deserialize(sessionJson);

        Boolean deleted = redisTemplate.delete(tokenKey);

        if (!Boolean.TRUE.equals(deleted)) {
            throw new InvalidRefreshTokenException(
                "Refresh token đã được sử dụng"
            );
        }

        CreatedRefreshToken newToken = createToken(
            oldSession.userId(),
            oldSession.email(),
            oldSession.role(),
            oldSession.familyId()
        );

        RefreshTokenSession newSession = findRequired(newToken.token());

        return new CreatedRefreshToken(newToken.token(), newSession);
    }

    @Override
    public void revoke(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            return;
        }

        String tokenKey = tokenKey(refreshToken);

        String sessionJson = redisTemplate.opsForValue().get(tokenKey);

        if (sessionJson == null) {
            return;
        }

        RefreshTokenSession session = deserialize(sessionJson);

        redisTemplate.delete(tokenKey);

        redisTemplate.opsForSet().remove(
            familyKey(session.familyId()),
            tokenKey
        );
    }

    @Override
    public void revokeFamily(UUID familyId) {
        String familyKey = familyKey(familyId);

        var tokenKeys = redisTemplate
            .opsForSet()
            .members(familyKey);

        if (tokenKeys != null && !tokenKeys.isEmpty()) {
            redisTemplate.delete(tokenKeys);
        }

        redisTemplate.delete(familyKey);
    }

    @Override
    public void writeToCookie(HttpServletResponse response, String refreshToken) {
        ResponseCookie cookie = ResponseCookie
            .from(cookieName, refreshToken)
            .httpOnly(true)
            .secure(secure)
            .sameSite(sameSite)
            .path("/api/v1/auth")
            .maxAge(expiration)
            .build();

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    @Override
    public void clearCookie(HttpServletResponse response) {
        ResponseCookie cookie = ResponseCookie
            .from(cookieName, "")
            .httpOnly(true)
            .secure(secure)
            .sameSite(sameSite)
            .path("/api/v1/auth")
            .maxAge(Duration.ZERO)
            .build();

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    private CreatedRefreshToken createToken(
        UUID userId,
        String email,
        String role,
        UUID familyId
    ) {
        String rawToken = generateSecureToken();

        Instant issuedAt = Instant.now();
        Instant expiresAt = issuedAt.plus(expiration);

        RefreshTokenSession session = new RefreshTokenSession(
            UUID.randomUUID(),
            familyId,
            userId,
            email,
            role,
            issuedAt,
            expiresAt
        );

        String tokenKey = tokenKey(rawToken);
        String familyKey = familyKey(familyId);

        redisTemplate.opsForValue().set(
            tokenKey,
            serialize(session),
            expiration
        );

        redisTemplate.opsForSet().add(
            familyKey,
            tokenKey
        );

        redisTemplate.expire(
            familyKey,
            expiration
        );

        return new CreatedRefreshToken(rawToken, session);
    }

    private RefreshTokenSession findRequired(String rawToken) {
        String json = redisTemplate
            .opsForValue()
            .get(tokenKey(rawToken));

        if (json == null) {
            throw new IllegalStateException(
                "Không thể tạo refresh session"
            );
        }

        return deserialize(json);
    }

    private String generateSecureToken() {
        byte[] bytes = new byte[64];
        secureRandom.nextBytes(bytes);

        return Base64.getUrlEncoder()
            .withoutPadding()
            .encodeToString(bytes);
    }

    private String tokenKey(String rawToken) {
        return TOKEN_PREFIX + sha256(rawToken);
    }

    private String familyKey(UUID familyId) {
        return FAMILY_PREFIX + familyId;
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");

            byte[] hash = digest.digest(
                value.getBytes(
                    StandardCharsets.UTF_8
                )
            );

            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(
                "SHA-256 không khả dụng",
                exception
            );
        }
    }

    private String serialize(RefreshTokenSession session) {
        try {
            return objectMapper.writeValueAsString(
                session
            );
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException(
                "Không thể serialize refresh session",
                exception
            );
        }
    }

    private RefreshTokenSession deserialize(String json) {
        try {
            return objectMapper.readValue(
                json,
                RefreshTokenSession.class
            );
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException(
                "Refresh session không hợp lệ",
                exception
            );
        }
    }
}