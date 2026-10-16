package com.sr.auth.service.impl;

import java.time.Instant;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import com.sr.auth.dto.AccessToken;
import com.sr.auth.dto.RefreshTokenSession;
import com.sr.auth.service.AccessTokenService;

@Service
public class AccessTokenServiceImpl implements AccessTokenService {

    private final JwtEncoder jwtEncoder;
    private final String issuer;
    private final long expirationSeconds;

    public AccessTokenServiceImpl(
        JwtEncoder jwtEncoder,
        @Value("${app.security.jwt.issuer}")
        String issuer,
        @Value("${app.security.jwt.access-token-expiration-seconds}")
        long expirationSeconds
    ) {
        this.jwtEncoder = jwtEncoder;
        this.issuer = issuer;
        this.expirationSeconds = expirationSeconds;
    }

    @Override
    public AccessToken create(RefreshTokenSession session) {
        Instant issuedAt = Instant.now();
        Instant expiresAt = issuedAt.plusSeconds(expirationSeconds);

        JwtClaimsSet claims = JwtClaimsSet.builder()
            .issuer(issuer)
            .issuedAt(issuedAt)
            .expiresAt(expiresAt)
            .subject(session.userId().toString())
            .id(UUID.randomUUID().toString())
            .claim("email", session.email())
            .claim("role", session.role())
            .claim("sessionId", session.sessionId().toString())
            .build();

        String token = jwtEncoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();

        return new AccessToken(token, expirationSeconds);
    }
}