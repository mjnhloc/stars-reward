package com.sr.config;

import java.util.Base64;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

@Configuration
public class JwtConfig {

    private SecretKey createSecretKey(String secret) {
        if (secret == null || secret.isBlank()) {
            throw new IllegalArgumentException("JWT_SECRET chưa được cấu hình");
        }

        byte[] secretBytes;

        try {
            secretBytes = Base64.getDecoder().decode(secret.trim());
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException(
                "JWT_SECRET không phải Base64 hợp lệ. ",
                exception
            );
        }

        if (secretBytes.length < 32) {
            throw new IllegalArgumentException("JWT secret phải có ít nhất 32 bytes sau khi decode");
        }

        return new SecretKeySpec(secretBytes, "HmacSHA256");
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public JwtEncoder jwtEncoder(
        @Value("${app.security.jwt.secret}")
        String secret
    ) {
        SecretKey secretKey = createSecretKey(secret);

        return NimbusJwtEncoder.withSecretKey(secretKey).algorithm(MacAlgorithm.HS256).build();
    }

    @Bean
    public JwtDecoder jwtDecoder(
        @Value("${app.security.jwt.secret}")
        String secret
    ) {
        SecretKey secretKey = createSecretKey(secret);

        return NimbusJwtDecoder.withSecretKey(secretKey).macAlgorithm(MacAlgorithm.HS256).build();
    }
}
