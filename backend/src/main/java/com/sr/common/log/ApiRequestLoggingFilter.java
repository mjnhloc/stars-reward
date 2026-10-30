package com.sr.common.log;

import java.io.IOException;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.annotation.Nonnull;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class ApiRequestLoggingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(
        ApiRequestLoggingFilter.class
    );

    @Override
    protected void doFilterInternal(
        @Nonnull
        HttpServletRequest request,
        @Nonnull
        HttpServletResponse response,
        @Nonnull
        FilterChain filterChain
    ) throws ServletException, IOException {

        long startedAt = System.currentTimeMillis();

        String requestId = request.getHeader(
            "X-Request-Id"
        );

        if (requestId == null || requestId.isBlank()) {
            requestId = UUID.randomUUID().toString();
        }

        response.setHeader(
            "X-Request-Id",
            requestId
        );

        try {
            filterChain.doFilter(request, response);
        } finally {
            long durationMs = System.currentTimeMillis() - startedAt;

            log.info(
                "HTTP {} {} status={} durationMs={} userId={} requestId={}",
                request.getMethod(),
                buildRequestPath(request),
                response.getStatus(),
                durationMs,
                getCurrentUserId(),
                requestId
            );
        }
    }

    private String buildRequestPath(HttpServletRequest request) {
        String queryString = request.getQueryString();

        if (queryString == null || queryString.isBlank()) {
            return request.getRequestURI();
        }

        return request.getRequestURI() + "?" + queryString;
    }

    private String getCurrentUserId() {
        Authentication authentication = SecurityContextHolder
            .getContext()
            .getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            return "anonymous";
        }

        if (authentication.getPrincipal() instanceof Jwt jwt) {
            return jwt.getSubject();
        }

        return authentication.getName();
    }
}