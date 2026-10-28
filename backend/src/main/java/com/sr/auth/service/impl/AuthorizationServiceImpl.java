package com.sr.auth.service.impl;

import java.util.Map;
import java.util.UUID;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sr.auth.dto.request.ChangePasswordRequest;
import com.sr.auth.dto.request.LoginRequest;
import com.sr.auth.dto.response.AuthenticatedUserResponse;
import com.sr.auth.dto.response.ChangePasswordResponse;
import com.sr.auth.service.AuthorizationService;
import com.sr.common.audit.dto.AuditLogCommand;
import com.sr.common.audit.service.AuditLogService;

@Service
public class AuthorizationServiceImpl implements AuthorizationService {
    private final JdbcClient db;
    private final PasswordEncoder passwordEncoder;
    private final AuditLogService auditLogService;

    public AuthorizationServiceImpl(
        JdbcClient db,
        PasswordEncoder passwordEncoder,
        AuditLogService auditLogService
    ) {
        this.db = db;
        this.passwordEncoder = passwordEncoder;
        this.auditLogService = auditLogService;
    }

    private void require(UUID userId, UUID classId, boolean write) {
        boolean ok = db.sql("""
                    SELECT EXISTS (
                        SELECT 1
                        FROM users u
                        WHERE u.id = :userId
                        AND u.active
                        AND (u.role = 'ADMIN' OR EXISTS (
                                SELECT 1
                                FROM class_user_assignments a
                                WHERE a.user_id = u.id
                                AND a.class_id = :classId
                                AND (NOT :w OR a.can_adjust)
                            )
                        )
                    )
            """)
            .param("userId", userId)
            .param("classId", classId)
            .param("w", write)
            .query(Boolean.class)
            .single();

        if (!ok) {
            throw new IllegalStateException("Bạn không có quyền trên lớp này");
        }
    }

    @Override
    public void requireView(UUID userId, UUID classId) {
        require(userId, classId, false);
    }

    @Override
    public void requireAdjust(UUID userId, UUID classId) {
        require(userId, classId, true);
    }

    @Override
    @Transactional(readOnly = true)
    public AuthenticatedUserResponse login(LoginRequest loginRequest) {
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
            .orElseThrow(() -> new BadCredentialsException("Tài khoản hoặc mật khẩu không chính xác"));

        if (!user.active() || user.passwordHash() == null
            || !passwordEncoder.matches(loginRequest.password(), user.passwordHash())) {
            throw new BadCredentialsException("Tài khoản hoặc mật khẩu không chính xác");
        }

        return user;
    }

    @Transactional
    @Override
    public ChangePasswordResponse changePassword(UUID userId, ChangePasswordRequest request) {
        if (!request.newPassword().equals(request.confirmPassword())) {
            throw new IllegalArgumentException("Xác nhận mật khẩu không khớp.");
        }

        String currPasswordHash = db.sql("""
            SELECT password_hash
            FROM users
            WHERE id = :userId
                AND active = TRUE
            FOR UPDATE
            """)
            .param("userId", userId)
            .query(String.class)
            .optional()
            .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy người dùng"));

        if (!passwordEncoder.matches(request.currentPassword(), currPasswordHash)) {
            throw new BadCredentialsException("Mật khẩu hiện tại không chính xác.");
        }

        if (passwordEncoder.matches(request.newPassword(), currPasswordHash)) {
            throw new BadCredentialsException("Mật khẩu mới phải khác mật khẩu hiện tại.");
        }

        String newPasswordHash = passwordEncoder.encode(request.newPassword());

        int updatedRows = db.sql("""
            UPDATE users
            SET password_hash = :newPasswordHash,
                updated_at = CURRENT_TIMESTAMP
            WHERE id = :userId
                AND active = TRUE
            """)
            .param("newPasswordHash", newPasswordHash)
            .param("userId", userId)
            .update();

        if (updatedRows != 1) {
            throw new IllegalStateException("Không thể cập nhật mật khẩu mới.");
        }

        /*
         * Write into audit log table
         */
        auditLogService.record(
            new AuditLogCommand(
                userId,
                "PASSWORD_CHANGED",
                "USER",
                userId,
                Map.of()
            )
        );

        return new ChangePasswordResponse("Đổi mật khẩu thành công!");
    }
}
