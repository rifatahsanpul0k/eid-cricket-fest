package com.eidcricketfest.common.exception;

import com.eidcricketfest.common.web.ApiErrorCode;
import com.eidcricketfest.common.web.ApiProblemFactory;
import com.eidcricketfest.common.web.FieldValidationError;
import com.eidcricketfest.common.web.RequestIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log =
            LoggerFactory.getLogger(
                    GlobalExceptionHandler.class
            );

    private final ApiProblemFactory problems;

    public GlobalExceptionHandler(
            ApiProblemFactory problems
    ) {
        this.problems = problems;
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ProblemDetail handleNotFound(
            ResourceNotFoundException ex,
            HttpServletRequest request
    ) {

        return problems.create(
                HttpStatus.NOT_FOUND,
                ApiErrorCode.NOT_FOUND,
                "Resource not found",
                ex.getMessage(),
                request
        );
    }

    @ExceptionHandler(ConflictException.class)
    public ProblemDetail handleConflict(
            ConflictException ex,
            HttpServletRequest request
    ) {

        return problems.create(
                HttpStatus.CONFLICT,
                ApiErrorCode.CONFLICT,
                "Conflict",
                ex.getMessage(),
                request
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(
            MethodArgumentNotValidException ex,
            HttpServletRequest request
    ) {

        ProblemDetail problem =
                problems.create(
                        HttpStatus.BAD_REQUEST,
                        ApiErrorCode.VALIDATION_ERROR,
                        "Validation failed",
                        "Request validation failed",
                        request
                );

        var errors =
                ex.getBindingResult()
                        .getFieldErrors()
                        .stream()
                        .map(error ->
                                new FieldValidationError(
                                        error.getField(),
                                        error.getDefaultMessage()
                                )
                        )
                        .toList();

        problem.setProperty(
                "errors",
                errors
        );

        return problem;
    }

    @ExceptionHandler(
            ConstraintViolationException.class
    )
    public ProblemDetail handleConstraintViolation(
            ConstraintViolationException ex,
            HttpServletRequest request
    ) {

        ProblemDetail problem =
                problems.create(
                        HttpStatus.BAD_REQUEST,
                        ApiErrorCode.VALIDATION_ERROR,
                        "Validation failed",
                        "Request validation failed",
                        request
                );

        var errors =
                ex.getConstraintViolations()
                        .stream()
                        .map(violation ->
                                new FieldValidationError(
                                        violation
                                                .getPropertyPath()
                                                .toString(),

                                        violation
                                                .getMessage()
                                )
                        )
                        .toList();

        problem.setProperty(
                "errors",
                errors
        );

        return problem;
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ProblemDetail handleUnauthorized(
            UnauthorizedException ex,
            HttpServletRequest request
    ) {

        return problems.create(
                HttpStatus.UNAUTHORIZED,
                ApiErrorCode.UNAUTHORIZED,
                "Unauthorized",
                ex.getMessage(),
                request
        );
    }

    @ExceptionHandler(ForbiddenException.class)
    public ProblemDetail handleForbidden(
            ForbiddenException ex,
            HttpServletRequest request
    ) {

        return problems.create(
                HttpStatus.FORBIDDEN,
                ApiErrorCode.FORBIDDEN,
                "Forbidden",
                ex.getMessage(),
                request
        );
    }

    @ExceptionHandler(
            RateLimitExceededException.class
    )
    public ProblemDetail handleRateLimitExceeded(
            RateLimitExceededException ex,
            HttpServletRequest request,
            HttpServletResponse response
    ) {

        response.setHeader(
                HttpHeaders.RETRY_AFTER,
                Long.toString(
                        ex.getRetryAfterSeconds()
                )
        );

        ProblemDetail problem =
                problems.create(
                        HttpStatus.TOO_MANY_REQUESTS,
                        ApiErrorCode.RATE_LIMITED,
                        "Too many requests",
                        "Too many requests. Try again later.",
                        request
                );

        problem.setProperty(
                "retryAfterSeconds",
                ex.getRetryAfterSeconds()
        );

        return problem;
    }

    @ExceptionHandler(
            DataIntegrityViolationException.class
    )
    public ProblemDetail handleDataIntegrity(
            DataIntegrityViolationException ex,
            HttpServletRequest request
    ) {

        return problems.create(
                HttpStatus.CONFLICT,
                ApiErrorCode.DATA_CONFLICT,
                "Data conflict",
                "The operation conflicts with existing data.",
                request
        );
    }

    @ExceptionHandler(
            OptimisticLockingFailureException.class
    )
    public ProblemDetail handleOptimisticLock(
            OptimisticLockingFailureException ex,
            HttpServletRequest request
    ) {

        return problems.create(
                HttpStatus.CONFLICT,
                ApiErrorCode.CONCURRENT_MODIFICATION,
                "Concurrent modification",
                "The resource was modified by another request. Reload and retry.",
                request
        );
    }

    @ExceptionHandler(
            HttpMessageNotReadableException.class
    )
    public ProblemDetail handleInvalidJson(
            HttpMessageNotReadableException ex,
            HttpServletRequest request
    ) {

        return problems.create(
                HttpStatus.BAD_REQUEST,
                ApiErrorCode.INVALID_REQUEST,
                "Invalid request body",
                "Request body contains invalid JSON or an invalid value.",
                request
        );
    }

    @ExceptionHandler(
            MethodArgumentTypeMismatchException.class
    )
    public ProblemDetail handleTypeMismatch(
            MethodArgumentTypeMismatchException ex,
            HttpServletRequest request
    ) {

        return problems.create(
                HttpStatus.BAD_REQUEST,
                ApiErrorCode.INVALID_REQUEST,
                "Invalid request parameter",
                "Invalid value for parameter: "
                        + ex.getName(),
                request
        );
    }

    @ExceptionHandler(
            IllegalArgumentException.class
    )
    public ProblemDetail handleIllegalArgument(
            IllegalArgumentException ex,
            HttpServletRequest request
    ) {

        return problems.create(
                HttpStatus.BAD_REQUEST,
                ApiErrorCode.INVALID_REQUEST,
                "Invalid request",
                ex.getMessage(),
                request
        );
    }

    @ExceptionHandler(
            IllegalStateException.class
    )
    public ProblemDetail handleIllegalState(
            IllegalStateException ex,
            HttpServletRequest request
    ) {

        return problems.create(
                HttpStatus.CONFLICT,
                ApiErrorCode.INVALID_STATE,
                "Invalid state",
                ex.getMessage(),
                request
        );
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnexpected(
            Exception ex,
            HttpServletRequest request
    ) {

        Object requestId =
                request.getAttribute(
                        RequestIdFilter.ATTRIBUTE_NAME
                );

        log.error(
                "Unexpected request failure requestId={}",
                requestId,
                ex
        );

        return problems.create(
                HttpStatus.INTERNAL_SERVER_ERROR,
                ApiErrorCode.INTERNAL_ERROR,
                "Internal server error",
                "An unexpected error occurred.",
                request
        );
    }
}
