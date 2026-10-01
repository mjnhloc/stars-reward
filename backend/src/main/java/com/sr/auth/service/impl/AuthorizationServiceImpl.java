package com.sr.auth.service.impl;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sr.auth.dto.request.LoginRequest;
import com.sr.auth.dto.response.AuthenticatedUserResponse;
import com.sr.auth.dto.response.LoginResponse;
import com.sr.auth.service.AuthorizationService;
import com.sr.user.dto.response.AuthUserResponse;

@Service
public class AuthorizationServiceImpl implements AuthorizationService {
    private final JdbcClient db;
    private final PasswordEncoder passwordEncoder;
    private final JwtEncoder jwtEncoder;
    private final long accessTokenExpirationSeconds;
    private final String issuer;

    public AuthorizationServiceImpl(
        JdbcClient db,
        PasswordEncoder passwordEncoder,
        JwtEncoder jwtEncoder,
        @Value("${app.security.jwt.access-token-expiration-seconds}")
        long accessTokenExpirationSeconds,
        @Value("${app.security.jwt.issuer}")
        String issuer
    ) {
        this.db = db;
        this.passwordEncoder = passwordEncoder;
        this.jwtEncoder = jwtEncoder;
        this.accessTokenExpirationSeconds = accessTokenExpirationSeconds;
        this.issuer = issuer;
    }

    private BadCredentialsException invalidCredentials() {
        return new BadCredentialsException("Tài khoản hoặc mật khẩu không chính xác");
    }

    private void require(UUID user, UUID classId, boolean write) {
        String sql = """
                    SELECT EXISTS (
                        SELECT 1
                        FROM users u
                        WHERE u.id=:u
                        AND u.active
                        AND (u.role='ADMIN' OR EXISTS (
                                SELECT 1
                                FROM class_user_assignments a
                                WHERE a.user_id=u.id
                                AND a.class_id=:c
                                AND (NOT :w OR a.can_adjust)
                            )
                        )
                    )
            """;

        boolean ok = db.sql(sql).param("u", user).param("c", classId).param("w", write).query(Boolean.class).single();
        if (!ok)
            throw new IllegalStateException("Bạn không có quyền trên lớp này");
    }

    @Override
    public void requireView(UUID user, UUID classId) {
        require(user, classId, false);
    }

    @Override
    public void requireAdjust(UUID user, UUID classId) {
        require(user, classId, true);
    }

    @Override
    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest loginRequest) {
        String normalizedEmail = loginRequest.email().trim().toLowerCase();

        AuthenticatedUserResponse user = db.sql("""
                    SELECT
                        id,
                        email,
                        password_hash,
                        display_name,
                        role,
                        active
                    FROM
                        users
                    WHERE LOWER(email) = :email
                    LIMIT 1
            """)
            .param("email", normalizedEmail)
            .query(
                (result, rowNum) -> new AuthenticatedUserResponse(
                    result.getObject("id", UUID.class),
                    result.getString("email"),
                    result.getString("password_hash"),
                    result.getString("display_name"),
                    result.getString("role"),
                    result.getBoolean("active")
                )
            )
            .optional()
            .orElseThrow(this::invalidCredentials);

        if (!user.active() || user.passwordHash() == null
            || !passwordEncoder.matches(loginRequest.password(), user.passwordHash())) {
            throw invalidCredentials();
        }

        long expiresIn = loginRequest.remember() ? accessTokenExpirationSeconds * 24 : accessTokenExpirationSeconds;

        Instant issuedAt = Instant.now();
        Instant expiresAt = issuedAt.plus(expiresIn, ChronoUnit.SECONDS);

        JwtClaimsSet claims = JwtClaimsSet.builder()
            .issuer(issuer)
            .issuedAt(issuedAt)
            .expiresAt(expiresAt)
            .subject(user.id().toString())
            .claim("email", user.email())
            .claim("displayName", user.displayName())
            .claim("role", user.role())
            .build();

        String accessToken = jwtEncoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();

        AuthUserResponse userResponse = new AuthUserResponse(
            user.id(),
            user.email(),
            user.displayName(),
            user.role()
        );

        return new LoginResponse(
            accessToken,
            "Bearer",
            expiresIn,
            userResponse
        );
    }
}
