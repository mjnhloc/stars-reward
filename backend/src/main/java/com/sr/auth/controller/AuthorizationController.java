package com.sr.auth.controller;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.CookieValue;
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

import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthorizationController {

    private final AuthorizationService authorizationService;
    private final UserService userService;
    private final RefreshTokenService refreshTokenService;
    private final AccessTokenService accessTokenService;

    public AuthorizationController(
        AuthorizationService authorizationService,
        UserService userService,
        RefreshTokenService refreshTokenService,
        AccessTokenService accessTokenService
    ) {
        this.authorizationService = authorizationService;
        this.userService = userService;
        this.refreshTokenService = refreshTokenService;
        this.accessTokenService = accessTokenService;
    }

    @GetMapping("/me")
    public AuthUserResponse getCurrentUser(
        @AuthenticationPrincipal
        Jwt jwt
    ) {
        UUID userId = UUID.fromString(jwt.getSubject());

        return userService.getCurrentUser(userId);
    }

    @PostMapping("/login")
    public LoginResponse login(
        @RequestBody
        @Valid
        LoginRequest loginRequest,
        HttpServletResponse response
    ) {
        AuthenticatedUserResponse user = authorizationService.login(loginRequest);

        CreatedRefreshToken refreshToken = refreshTokenService.create(user.id(), user.email(), user.role());
        AccessToken accessToken = accessTokenService.create(refreshToken.session());

        refreshTokenService.writeToCookie(response, refreshToken.token());

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

    // revoke refresh token and clear token from cookie
    // the access token still work for before the expiration
    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(
        @CookieValue(name = "${app.security.refresh-token.cookie-name}", required = false)
        String refreshToken, HttpServletResponse response
    ) {
        refreshTokenService.revoke(refreshToken);
        refreshTokenService.clearCookie(response);
    }

    @PutMapping("/change-password")
    public ChangePasswordResponse putMethodName(
        @AuthenticationPrincipal
        Jwt jwt,
        @Valid
        @RequestBody
        ChangePasswordRequest request
    ) {
        UUID userId = UUID.fromString(jwt.getSubject());

        return authorizationService.changePassword(userId, request);
    }

    @PostMapping("/refresh")
    public TokenResponse refresh(
        @CookieValue(name = "${app.security.refresh-token.cookie-name}")
        String refreshToken, HttpServletResponse response
    ) {
        CreatedRefreshToken rotatedRefreshToken = refreshTokenService.rotate(refreshToken);

        AccessToken accessToken = accessTokenService.create(rotatedRefreshToken.session());

        refreshTokenService.writeToCookie(response, accessToken.value());

        return new TokenResponse(
            accessToken.value(),
            "Bearer",
            accessToken.expiresIn()
        );
    }

}
