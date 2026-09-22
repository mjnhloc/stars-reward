package com.sr.user.service.impl;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

import com.sr.user.service.UserService;

@Service
public class UserServiceImpl implements UserService {
    private static final UUID LOCAL_ADMIN = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private final boolean securityEnabled;

    public UserServiceImpl(
        @Value("${app.security.enabled:false}") boolean enabled
    ) {
        this.securityEnabled = enabled;
    }

    @Override
    public UUID getUserId() {
        if (!securityEnabled)
            return LOCAL_ADMIN;
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof Jwt jwt))
            throw new IllegalStateException("Chưa đăng nhập");
        Object id = jwt.getClaim("app_user_id");
        if (id == null)
            throw new IllegalStateException("JWT thiếu app_user_id");
        return UUID.fromString(id.toString());
    }
}
