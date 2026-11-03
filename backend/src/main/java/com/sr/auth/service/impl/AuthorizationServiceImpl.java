package com.sr.auth.service.impl;

import java.util.Map;
import java.util.UUID;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sr.auth.dto.request.ChangePasswordRequest;
import com.sr.auth.dto.request.LoginRequest;
import com.sr.auth.dto.response.AuthenticatedUserResponse;
import com.sr.auth.dto.response.ChangePasswordResponse;
import com.sr.auth.service.AuthorizationService;
import com.sr.classes.repository.ClassUserAssignmentRepository;
import com.sr.common.audit.dto.AuditLogCommand;
import com.sr.common.audit.service.AuditLogService;
import com.sr.user.entity.UserEntity;
import com.sr.user.repository.UserRepository;

@Service
public class AuthorizationServiceImpl implements AuthorizationService {
    private final UserRepository userRepository;
    private final ClassUserAssignmentRepository classUserAssignmentRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditLogService auditLogService;

    public AuthorizationServiceImpl(
        UserRepository userRepository,
        ClassUserAssignmentRepository classUserAssignmentRepository,
        PasswordEncoder passwordEncoder,
        AuditLogService auditLogService
    ) {
        this.userRepository = userRepository;
        this.classUserAssignmentRepository = classUserAssignmentRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditLogService = auditLogService;
    }

    /*
     * Active ADMIN can access every class, other users need an assignment to the
     * class (and can_adjust when writing)
     */
    private void require(UUID userId, UUID classId, boolean write) {
        boolean ok = userRepository.findByIdAndActiveTrue(userId)
            .map(
                user -> "ADMIN".equals(user.getRole())
                    || (write
                        ? classUserAssignmentRepository.existsByUserIdAndClassIdAndCanAdjustTrue(userId, classId)
                        : classUserAssignmentRepository.existsByUserIdAndClassId(userId, classId))
            )
            .orElse(false);

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

        AuthenticatedUserResponse user = userRepository.findByNormalizedEmail(normalizedEmail)
            .map(
                entity -> new AuthenticatedUserResponse(
                    entity.getId(),
                    entity.getEmail(),
                    entity.getPasswordHash(),
                    entity.getDisplayName(),
                    entity.getRole(),
                    entity.isActive()
                )
            )
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

        UserEntity user = userRepository.findActiveForUpdate(userId)
            .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy người dùng"));

        String currPasswordHash = user.getPasswordHash();

        if (!passwordEncoder.matches(request.currentPassword(), currPasswordHash)) {
            throw new BadCredentialsException("Mật khẩu hiện tại không chính xác.");
        }

        if (passwordEncoder.matches(request.newPassword(), currPasswordHash)) {
            throw new BadCredentialsException("Mật khẩu mới phải khác mật khẩu hiện tại.");
        }

        /*
         * Entity is managed, Hibernate writes the UPDATE when the transaction commits
         * (updated_at is set by @UpdateTimestamp)
         */
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));

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
