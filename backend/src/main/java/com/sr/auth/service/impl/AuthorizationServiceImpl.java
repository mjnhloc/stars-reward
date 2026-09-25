package com.sr.auth.service.impl;

import java.util.UUID;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;

import com.sr.auth.dto.response.LoginResponse;
import com.sr.auth.service.AuthorizationService;

@Service
public class AuthorizationServiceImpl implements AuthorizationService {
    private final JdbcClient db;

    public AuthorizationServiceImpl(JdbcClient db) {
        this.db = db;
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
    public LoginResponse login(String username, String password) {

        return new LoginResponse();
    }
}