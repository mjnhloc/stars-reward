package com.sr.user.service.impl;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sr.auth.service.RefreshTokenService;
import com.sr.classes.entity.ClassEntity;
import com.sr.classes.entity.ClassUserAssignmentEntity;
import com.sr.classes.repository.ClassRepository;
import com.sr.classes.repository.ClassUserAssignmentRepository;
import com.sr.common.audit.dto.AuditLogCommand;
import com.sr.common.audit.service.AuditLogService;
import com.sr.user.dto.request.CreateUserRequest;
import com.sr.user.dto.request.UpdateUserRequest;
import com.sr.user.dto.response.AuthUserResponse;
import com.sr.user.dto.response.UserResponse;
import com.sr.user.entity.UserEntity;
import com.sr.user.repository.UserRepository;
import com.sr.user.service.UserService;

@Service
public class UserServiceImpl implements UserService {

    private static final String ADMIN = "ADMIN";
    private static final String TEACHER = "TEACHER";

    private final UserRepository userRepository;
    private final ClassRepository classRepository;
    private final ClassUserAssignmentRepository classUserAssignmentRepository;
    private final RefreshTokenService refreshTokenService;
    private final PasswordEncoder passwordEncoder;
    private final AuditLogService auditLogService;

    public UserServiceImpl(
        UserRepository userRepository,
        ClassRepository classRepository,
        ClassUserAssignmentRepository classUserAssignmentRepository,
        RefreshTokenService refreshTokenService,
        PasswordEncoder passwordEncoder,
        AuditLogService auditLogService
    ) {
        this.userRepository = userRepository;
        this.classRepository = classRepository;
        this.classUserAssignmentRepository = classUserAssignmentRepository;
        this.refreshTokenService = refreshTokenService;
        this.passwordEncoder = passwordEncoder;
        this.auditLogService = auditLogService;
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
        return userRepository.findByIdAndActiveTrue(userId)
            .map(
                user -> new AuthUserResponse(
                    user.getId(),
                    user.getEmail(),
                    user.getDisplayName(),
                    user.getRole()
                )
            )
            .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy người dùng"));
    }

    /*
     * Function to list every user with the classes assigned to teachers
     */
    @Override
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional(readOnly = true)
    public List<UserResponse> getUsers() {
        List<UserEntity> users = userRepository.findAllByOrderByCreatedAtAsc();

        Map<UUID, List<UUID>> classesByUser = users.isEmpty()
            ? Map.of()
            : classUserAssignmentRepository.findByUserIdIn(users.stream().map(UserEntity::getId).toList())
                .stream()
                .collect(
                    Collectors.groupingBy(
                        ClassUserAssignmentEntity::getUserId,
                        Collectors.mapping(ClassUserAssignmentEntity::getClassId, Collectors.toList())
                    )
                );

        return users.stream()
            .map(user -> toResponse(user, classesByUser.getOrDefault(user.getId(), List.of())))
            .toList();
    }

    /*
     * Function to create a user, admins can also create other admins
     */
    @Override
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public UserResponse createUser(CreateUserRequest request) {
        UUID actorId = getUserId();

        String email = normalizeEmail(request.email());
        String displayName = request.displayName().trim();

        requireUniqueEmail(email, null);

        UserEntity user = UserEntity.create(
            displayName,
            email,
            passwordEncoder.encode(request.password()),
            request.role()
        );

        try {
            userRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException exception) {
            throw new IllegalArgumentException("Email đã được sử dụng");
        }

        List<UUID> classIds = replaceAssignments(user.getId(), request.role(), request.classIds());

        auditLogService.record(
            new AuditLogCommand(
                actorId,
                "USER_CREATED",
                "USER",
                user.getId(),
                Map.of(
                    "email",
                    email,
                    "displayName",
                    displayName,
                    "role",
                    request.role(),
                    "classIds",
                    classIds.stream().map(UUID::toString).toList()
                )
            )
        );

        return toResponse(user, classIds);
    }

    /*
     * Function to edit a user: name, email, role, lock or unlock and reset the
     * password. The user is logged out when it is locked, gets another role or a
     * new password
     */
    @Override
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public UserResponse updateUser(UUID userId, UpdateUserRequest request) {
        UUID actorId = getUserId();

        UserEntity user = userRepository.findForUpdate(userId)
            .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy người dùng"));

        String email = normalizeEmail(request.email());
        String displayName = request.displayName().trim();
        boolean active = request.active();
        boolean roleChanged = !user.getRole().equals(request.role());
        boolean deactivated = user.isActive() && !active;

        if (userId.equals(actorId) && (roleChanged || !active)) {
            throw new IllegalArgumentException("Bạn không thể tự khóa hoặc tự đổi vai trò của chính mình");
        }

        boolean losesAdmin = ADMIN.equals(user.getRole()) && user.isActive() && (roleChanged || !active);

        if (losesAdmin && userRepository.countByRoleAndActiveTrue(ADMIN) <= 1) {
            throw new IllegalArgumentException("Phải còn ít nhất một quản trị viên đang hoạt động");
        }

        requireUniqueEmail(email, userId);

        Map<String, Object> details = new HashMap<>();
        details.put(
            "before",
            Map.of(
                "email",
                user.getEmail(),
                "displayName",
                user.getDisplayName(),
                "role",
                user.getRole(),
                "active",
                user.isActive()
            )
        );
        details.put(
            "after",
            Map.of(
                "email",
                email,
                "displayName",
                displayName,
                "role",
                request.role(),
                "active",
                active
            )
        );

        boolean passwordReset = request.newPassword() != null;

        user.setDisplayName(displayName);
        user.setEmail(email);
        user.setRole(request.role());
        user.setActive(active);

        if (passwordReset) {
            user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        }

        try {
            userRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException exception) {
            throw new IllegalArgumentException("Email đã được sử dụng");
        }

        List<UUID> classIds = replaceAssignments(userId, request.role(), request.classIds());

        details.put("classIds", classIds.stream().map(UUID::toString).toList());
        details.put("passwordReset", passwordReset);

        /*
         * Refresh sessions keep the old role, so the user has to log in again
         */
        if (deactivated || roleChanged || passwordReset) {
            refreshTokenService.revokeAllByUserId(userId);
        }

        auditLogService.record(
            new AuditLogCommand(
                actorId,
                "USER_UPDATED",
                "USER",
                userId,
                details
            )
        );

        return toResponse(user, classIds);
    }

    /*
     * Teachers can adjust stars and redeem rewards only in their classes, other
     * roles do not need any class
     */
    private List<UUID> replaceAssignments(UUID userId, String role, Collection<UUID> requestedClassIds) {
        classUserAssignmentRepository.deleteAllOfUser(userId);

        if (!TEACHER.equals(role) || requestedClassIds == null || requestedClassIds.isEmpty()) {
            return List.of();
        }

        Set<UUID> classIds = new LinkedHashSet<>(requestedClassIds);

        Set<UUID> activeClassIds = classRepository.findAllById(classIds)
            .stream()
            .filter(classEntity -> classEntity.getArchivedAt() == null)
            .map(ClassEntity::getId)
            .collect(Collectors.toSet());

        if (!activeClassIds.containsAll(classIds)) {
            throw new IllegalArgumentException("Có lớp phụ trách không tồn tại hoặc đã bị xóa");
        }

        classUserAssignmentRepository.saveAll(
            classIds.stream().map(classId -> ClassUserAssignmentEntity.create(userId, classId, true)).toList()
        );

        return List.copyOf(classIds);
    }

    private void requireUniqueEmail(String email, UUID userId) {
        userRepository.findByNormalizedEmail(email)
            .filter(existing -> !existing.getId().equals(userId))
            .ifPresent(existing -> {
                throw new IllegalArgumentException("Email đã được sử dụng");
            });
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private UserResponse toResponse(UserEntity user, List<UUID> classIds) {
        return new UserResponse(
            user.getId(),
            user.getDisplayName(),
            user.getEmail(),
            user.getRole(),
            user.isActive(),
            user.getCreatedAt(),
            classIds
        );
    }
}
