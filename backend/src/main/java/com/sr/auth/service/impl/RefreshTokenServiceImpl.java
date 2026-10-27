package com.sr.auth.service.impl;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Set;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;

import com.sr.auth.dto.CreatedRefreshToken;
import com.sr.auth.dto.RefreshTokenSession;
import com.sr.auth.service.RefreshTokenService;
import com.sr.common.exception.InvalidRefreshTokenException;

import jakarta.servlet.http.HttpServletResponse;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

@Service
public class RefreshTokenServiceImpl implements RefreshTokenService {

    private static final String TOKEN_PREFIX = "refresh-token:";

    private static final String FAMILY_PREFIX = "refresh-family:";

    private static final String USER_PREFIX = "refresh-user:";

    private final StringRedisTemplate redisTemplate;
    private final JsonMapper jsonMapper;
    private final SecureRandom secureRandom;

    private final Duration sessionExpiration;
    private final Duration rememberExpiration;

    private final String cookieName;
    private final boolean secure;
    private final String sameSite;

    public RefreshTokenServiceImpl(
        StringRedisTemplate redisTemplate,
        JsonMapper jsonMapper,

        @Value("${app.security.refresh-token.session-expiration-seconds}")
        long sessionExpirationSeconds,

        @Value("${app.security.refresh-token.remember-expiration-seconds}")
        long rememberExpirationSeconds,

        @Value("${app.security.refresh-token.cookie-name}")
        String cookieName,

        @Value("${app.security.refresh-token.secure}")
        boolean secure,

        @Value("${app.security.refresh-token.same-site}")
        String sameSite
    ) {
        this.redisTemplate = redisTemplate;
        this.jsonMapper = jsonMapper;
        this.secureRandom = new SecureRandom();

        this.sessionExpiration = Duration.ofSeconds(sessionExpirationSeconds);

        this.rememberExpiration = Duration.ofSeconds(rememberExpirationSeconds);

        this.cookieName = cookieName;
        this.secure = secure;
        this.sameSite = sameSite;
    }

    @Override
    public CreatedRefreshToken create(
        UUID userId,
        String email,
        String role,
        boolean remember
    ) {
        Duration tokenLifetime = remember ? rememberExpiration : sessionExpiration;

        Instant issuedAt = Instant.now();
        Instant expiresAt = issuedAt.plus(tokenLifetime);

        return createToken(
            userId,
            email,
            role,
            UUID.randomUUID(),
            remember,
            issuedAt,
            expiresAt
        );
    }

    @Override
    public CreatedRefreshToken rotate(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new InvalidRefreshTokenException(
                "Không tìm thấy refresh token"
            );
        }

        String oldTokenKey = tokenKey(refreshToken);

        /*
         * Lấy session và xóa token cũ trong một operation. Hai request refresh đồng
         * thời sẽ không thể cùng sử dụng thành công một refresh token.
         */
        String sessionJson = redisTemplate
            .opsForValue()
            .getAndDelete(oldTokenKey);

        if (sessionJson == null) {
            throw new InvalidRefreshTokenException(
                "Refresh token không hợp lệ, đã sử dụng hoặc đã hết hạn"
            );
        }

        RefreshTokenSession oldSession = deserialize(sessionJson);

        Instant now = Instant.now();

        if (!oldSession.expiresAt().isAfter(now)) {
            removeFromIndexes(
                oldTokenKey,
                oldSession
            );

            throw new InvalidRefreshTokenException(
                "Phiên đăng nhập đã hết hạn"
            );
        }

        /*
         * Xóa token cũ khỏi các Redis Set index.
         */
        removeFromIndexes(
            oldTokenKey,
            oldSession
        );

        /*
         * Token mới giữ nguyên expiresAt ban đầu. Việc refresh không kéo dài phiên vô
         * hạn.
         */
        return createToken(
            oldSession.userId(),
            oldSession.email(),
            oldSession.role(),
            oldSession.familyId(),
            oldSession.remember(),
            now,
            oldSession.expiresAt()
        );
    }

    @Override
    public void revoke(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            return;
        }

        String tokenKey = tokenKey(refreshToken);

        String sessionJson = redisTemplate
            .opsForValue()
            .getAndDelete(tokenKey);

        if (sessionJson == null) {
            return;
        }

        RefreshTokenSession session = deserialize(sessionJson);

        removeFromIndexes(tokenKey, session);
    }

    @Override
    public void revokeFamily(UUID familyId) {
        String familyKey = familyKey(familyId);

        Set<String> tokenKeys = redisTemplate
            .opsForSet()
            .members(familyKey);

        if (tokenKeys != null) {
            for (String tokenKey : tokenKeys) {
                String sessionJson = redisTemplate
                    .opsForValue()
                    .getAndDelete(tokenKey);

                if (sessionJson != null) {
                    RefreshTokenSession session = deserialize(sessionJson);

                    redisTemplate
                        .opsForSet()
                        .remove(
                            userKey(session.userId()),
                            tokenKey
                        );
                }
            }
        }

        redisTemplate.delete(familyKey);
    }

    @Override
    public void revokeAllByUserId(UUID userId) {
        String userKey = userKey(userId);

        Set<String> tokenKeys = redisTemplate
            .opsForSet()
            .members(userKey);

        if (tokenKeys != null) {
            for (String tokenKey : tokenKeys) {
                String sessionJson = redisTemplate
                    .opsForValue()
                    .getAndDelete(tokenKey);

                if (sessionJson != null) {
                    RefreshTokenSession session = deserialize(sessionJson);

                    redisTemplate
                        .opsForSet()
                        .remove(
                            familyKey(session.familyId()),
                            tokenKey
                        );
                }
            }
        }

        redisTemplate.delete(userKey);
    }

    @Override
    public void writeToCookie(
        HttpServletResponse response,
        String refreshToken,
        boolean remember,
        Instant expiresAt
    ) {
        ResponseCookie.ResponseCookieBuilder builder = ResponseCookie
            .from(cookieName, refreshToken)
            .httpOnly(true)
            .secure(secure)
            .sameSite(sameSite)
            .path("/api/v1/auth");

        /*
         * Có Remember me: tạo persistent cookie với Max-Age.
         *
         * Không Remember me: không đặt Max-Age, cookie trở thành browser session
         * cookie.
         */
        if (remember) {
            Duration remaining = Duration.between(
                Instant.now(),
                expiresAt
            );

            if (remaining.isNegative() || remaining.isZero()) {
                throw new InvalidRefreshTokenException(
                    "Refresh session đã hết hạn"
                );
            }

            builder.maxAge(remaining);
        }

        response.addHeader(
            HttpHeaders.SET_COOKIE,
            builder.build().toString()
        );
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

        response.addHeader(
            HttpHeaders.SET_COOKIE,
            cookie.toString()
        );
    }

    private CreatedRefreshToken createToken(
        UUID userId,
        String email,
        String role,
        UUID familyId,
        boolean remember,
        Instant issuedAt,
        Instant expiresAt
    ) {
        Duration remainingTtl = Duration.between(Instant.now(), expiresAt);

        if (remainingTtl.isNegative() || remainingTtl.isZero()) {
            throw new InvalidRefreshTokenException(
                "Không thể tạo refresh token cho phiên đã hết hạn"
            );
        }

        String rawToken = generateSecureToken();

        RefreshTokenSession session = new RefreshTokenSession(
            UUID.randomUUID(),
            familyId,
            userId,
            email,
            role,
            remember,
            issuedAt,
            expiresAt
        );

        String tokenKey = tokenKey(rawToken);
        String familyKey = familyKey(familyId);
        String userKey = userKey(userId);

        redisTemplate.opsForValue().set(
            tokenKey,
            serialize(session),
            remainingTtl
        );

        redisTemplate.opsForSet().add(
            familyKey,
            tokenKey
        );

        redisTemplate.opsForSet().add(
            userKey,
            tokenKey
        );

        /*
         * Redis tự xóa các index khi hết TTL. Spring Data Redis hỗ trợ đặt thời gian
         * hết hạn bằng Duration.
         */
        redisTemplate.expire(
            familyKey,
            remainingTtl
        );

        redisTemplate.expire(
            userKey,
            remainingTtl
        );

        return new CreatedRefreshToken(
            rawToken,
            session
        );
    }

    private void removeFromIndexes(String tokenKey, RefreshTokenSession session) {
        redisTemplate.opsForSet().remove(
            familyKey(session.familyId()),
            tokenKey
        );

        redisTemplate.opsForSet().remove(
            userKey(session.userId()),
            tokenKey
        );
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

    private String userKey(UUID userId) {
        return USER_PREFIX + userId;
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance(
                "SHA-256"
            );

            byte[] hash = digest.digest(
                value.getBytes(
                    StandardCharsets.UTF_8
                )
            );

            return HexFormat.of()
                .formatHex(hash);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(
                "SHA-256 không khả dụng",
                exception
            );
        }
    }

    private String serialize(
        RefreshTokenSession session
    ) {
        try {
            return jsonMapper.writeValueAsString(
                session
            );
        } catch (JacksonException exception) {
            throw new IllegalStateException(
                "Không thể serialize refresh session",
                exception
            );
        }
    }

    private RefreshTokenSession deserialize(
        String json
    ) {
        try {
            return jsonMapper.readValue(
                json,
                RefreshTokenSession.class
            );
        } catch (JacksonException exception) {
            throw new IllegalStateException(
                "Refresh session không hợp lệ",
                exception
            );
        }
    }
}