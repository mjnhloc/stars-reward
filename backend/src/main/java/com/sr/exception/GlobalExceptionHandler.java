package com.sr.exception;

import java.time.OffsetDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.sr.exception.dto.ApiErrorResponse;

import jakarta.servlet.http.HttpServletRequest;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiErrorResponse> handleBadCredentials(
        BadCredentialsException exception,
        HttpServletRequest request
    ) {
        ApiErrorResponse response = new ApiErrorResponse(
            HttpStatus.UNAUTHORIZED.value(),
            "INVALID_CREDENTIALS",
            exception.getMessage(),
            request.getRequestURI(),
            OffsetDateTime.now()
        );

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiErrorResponse> handleIllegalArgument(
        IllegalArgumentException exception,
        HttpServletRequest request
    ) {
        ApiErrorResponse response = new ApiErrorResponse(
            HttpStatus.BAD_REQUEST.value(),
            "INVALID_REQUEST",
            exception.getMessage(),
            request.getRequestURI(),
            OffsetDateTime.now()
        );

        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleUnexpectedException(
        Exception exception,
        HttpServletRequest request
    ) {
        log.error(
            "Unexpected error while processing {} {}",
            request.getMethod(),
            request.getRequestURI(),
            exception
        );

        ApiErrorResponse response = new ApiErrorResponse(
            HttpStatus.INTERNAL_SERVER_ERROR.value(),
            "INTERNAL_SERVER_ERROR",
            "Something wrong happened, please try again.",
            request.getRequestURI(),
            OffsetDateTime.now()
        );

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}
