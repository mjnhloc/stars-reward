package com.sr.user.service.impl;

import java.util.UUID;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sr.user.dto.response.AuthUserResponse;
import com.sr.user.service.UserService;

@Service
public class UserServiceImpl implements UserService {
    private final JdbcClient jdbcClient;

    public UserServiceImpl(
        JdbcClient jdbcClient
    ) {
        this.jdbcClient = jdbcClient;
    }

    @Override
    public UUID getUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new AuthenticationCredentialsNotFoundException("Người dùng chưa đăng nhập");
        }

        if (!(auth.getPrincipal() instanceof Jwt jwt)) {
            throw new IllegalStateException("Thông tin xác thực không phải JWT");
        }

        String subject = jwt.getSubject();
        if (subject == null || subject.isBlank()) {
            throw new IllegalStateException("JWT không chứa subject");
        }

        try {
            return UUID.fromString(subject);
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException("JWT subject không phải UUID hợp lệ", exception);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public AuthUserResponse getCurrentUser(UUID userId) {
        return jdbcClient.sql("""
            SELECT
                id,
                email,
                display_name,
                role
            FROM users
            WHERE id = :userId
                AND active = true
            """)
            .param("userId", userId)
            .query(
                (resultSet, rowNum) -> new AuthUserResponse(
                    resultSet.getObject("id", UUID.class),
                    resultSet.getString("email"),
                    resultSet.getString("display_name"),
                    resultSet.getString("role")
                )
            )
            .optional()
            .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy người dùng"));
    }
}
