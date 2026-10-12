package com.aadi.sandesh.exception;

import com.aadi.sandesh.dto.common.ApiError;
import com.aadi.sandesh.dto.common.FieldViolation;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.nio.file.AccessDeniedException;
import java.time.Instant;
import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = org.slf4j.LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(NotFoundException.class)
    ResponseEntity<ApiError> handleNotFoundException(NotFoundException ex, HttpServletRequest request) {
        return error(HttpStatus.NOT_FOUND, ex.getMessage(), "NOT_FOUND", request);
    }


    @ExceptionHandler({ForbiddenException.class, AccessDeniedException.class})
    ResponseEntity<ApiError> handleForbiddenException(RuntimeException e, HttpServletRequest request) {
        return error(HttpStatus.FORBIDDEN, e.getMessage(), "FORBIDDEN", request);
    }


    @ExceptionHandler(ConflictException.class)
    ResponseEntity<ApiError> handleConflictException(ConflictException ex, HttpServletRequest request) {
        return error(HttpStatus.CONFLICT, ex.getMessage(), "CONFLICT", request);
    }

    @ExceptionHandler(RateLimitExceededException.class)
    ResponseEntity<ApiError> handleRateLimitExceededException(RateLimitExceededException ex, HttpServletRequest request) {
        return error(HttpStatus.TOO_MANY_REQUESTS, ex.getMessage(), "RATE_LIMIT_EXCEEDED", request);
    }


    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiError> handleMethodArgumentNotValidException(MethodArgumentNotValidException ex, HttpServletRequest request) {

        var violations = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> new FieldViolation(error.getField(), error.getDefaultMessage()))
                .toList();
        var body = new ApiError(
                Instant.now(),
                HttpStatus.BAD_REQUEST.value(),
                "VALIDATION_ERROR",
                "Validation failed for one or more fields",
                request.getRequestURI(),
                violations
        );
        return ResponseEntity.badRequest().body(body);
    }


    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiError> handleGenericException(Exception ex, HttpServletRequest request) {
        logger.error(ex.getMessage(),ex);
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "An Unexpected Error Occurred", "INTERNAL_SERVER_ERROR", request);
    }



    private ResponseEntity<ApiError> error(HttpStatus status,String message, String code,
                                           HttpServletRequest request) {
        var body = new ApiError(
                Instant.now(),
                status.value(),
                code,
                message,
                request.getRequestURI(),
                List.of()
        );

        return new ResponseEntity<>(body, status);
    }
}
