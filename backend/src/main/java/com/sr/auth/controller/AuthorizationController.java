package com.sr.auth.controller;

import java.util.Arrays;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.sr.auth.dto.AccessToken;
import com.sr.auth.dto.CreatedRefreshToken;
import com.sr.auth.dto.request.ChangePasswordRequest;
import com.sr.auth.dto.request.LoginRequest;
import com.sr.auth.dto.response.AuthenticatedUserResponse;
import com.sr.auth.dto.response.ChangePasswordResponse;
import com.sr.auth.dto.response.LoginResponse;
import com.sr.auth.dto.response.TokenResponse;
import com.sr.auth.service.AccessTokenService;
import com.sr.auth.service.AuthorizationService;
import com.sr.auth.service.RefreshTokenService;
import com.sr.user.dto.response.AuthUserResponse;
import com.sr.user.service.UserService;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthorizationController {

    private final AuthorizationService authorizationService;
    private final UserService userService;
    private final RefreshTokenService refreshTokenService;
    private final AccessTokenService accessTokenService;
    private final String refreshTokenCookieName;

    public AuthorizationController(
        AuthorizationService authorizationService,
        UserService userService,
        RefreshTokenService refreshTokenService,
        AccessTokenService accessTokenService,
        @Value("${app.security.refresh-token.cookie-name}")
        String refreshTokenCookieName
    ) {
        this.authorizationService = authorizationService;
        this.userService = userService;
        this.refreshTokenService = refreshTokenService;
        this.accessTokenService = accessTokenService;
        this.refreshTokenCookieName = refreshTokenCookieName;
    }

    @GetMapping("/me")
    public AuthUserResponse getCurrentUser(
        @AuthenticationPrincipal
        Jwt jwt
    ) {
        UUID userId = extractUserId(jwt);

        return userService.getCurrentUser(userId);
    }

    @PostMapping("/login")
    public LoginResponse login(
        @Valid
        @RequestBody
        LoginRequest request,
        HttpServletResponse response
    ) {
        AuthenticatedUserResponse user = authorizationService.login(request);

        CreatedRefreshToken refreshToken = refreshTokenService.create(
            user.id(),
            user.email(),
            user.role(),
            request.remember()
        );

        AccessToken accessToken = accessTokenService.create(
            refreshToken.session()
        );

        refreshTokenService.writeToCookie(
            response,
            refreshToken.token(),
            refreshToken.session().remember(),
            refreshToken.session().expiresAt()
        );

        AuthUserResponse userResponse = new AuthUserResponse(
            user.id(),
            user.email(),
            user.displayName(),
            user.role()
        );

        return new LoginResponse(
            accessToken.value(),
            "Bearer",
            accessToken.expiresIn(),
            userResponse
        );
    }

    @PostMapping("/refresh")
    public TokenResponse refresh(
        HttpServletRequest request,
        HttpServletResponse response
    ) {
        String currentRefreshToken = findRefreshTokenCookie(request);

        if (currentRefreshToken == null || currentRefreshToken.isBlank()) {
            throw new IllegalArgumentException(
                "Không tìm thấy refresh token"
            );
        }

        CreatedRefreshToken rotatedRefreshToken = refreshTokenService.rotate(
            currentRefreshToken
        );

        AccessToken accessToken = accessTokenService.create(
            rotatedRefreshToken.session()
        );

        refreshTokenService.writeToCookie(
            response,
            rotatedRefreshToken.token(),
            rotatedRefreshToken.session().remember(),
            rotatedRefreshToken.session().expiresAt()
        );

        return new TokenResponse(
            accessToken.value(),
            "Bearer",
            accessToken.expiresIn()
        );
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(
        HttpServletRequest request,
        HttpServletResponse response
    ) {
        String refreshToken = findRefreshTokenCookie(request);

        if (refreshToken != null && !refreshToken.isBlank()) {
            refreshTokenService.revoke(refreshToken);
        }

        /*
         * Luôn xóa cookie, kể cả khi Redis key đã hết hạn hoặc token không hợp lệ.
         */
        refreshTokenService.clearCookie(response);
    }

    @PutMapping("/change-password")
    public ChangePasswordResponse changePassword(
        @AuthenticationPrincipal
        Jwt jwt,
        @Valid
        @RequestBody
        ChangePasswordRequest request,
        HttpServletResponse response
    ) {
        UUID userId = extractUserId(jwt);

        ChangePasswordResponse result = authorizationService.changePassword(userId, request);

        /*
         * Nên thu hồi toàn bộ refresh session sau khi đổi mật khẩu.
         */
        refreshTokenService.revokeAllByUserId(userId);
        refreshTokenService.clearCookie(response);

        return result;
    }

    private UUID extractUserId(Jwt jwt) {
        if (jwt == null || jwt.getSubject() == null) {
            throw new IllegalArgumentException(
                "Access token không chứa user ID"
            );
        }

        return UUID.fromString(jwt.getSubject());
    }

    private String findRefreshTokenCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();

        if (cookies == null) {
            return null;
        }

        return Arrays.stream(cookies)
            .filter(
                cookie -> refreshTokenCookieName.equals(
                    cookie.getName()
                )
            )
            .map(Cookie::getValue)
            .findFirst()
            .orElse(null);
    }
}
