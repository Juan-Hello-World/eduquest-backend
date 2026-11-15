package com.eduquest.api.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final String PROBLEM_TIMESTAMP = "timestamp";
    private static final String PROBLEM_ERROR = "error";
    private static final String PROBLEM_MESSAGE = "message";
    private static final String PROBLEM_PATH = "path";

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private ProblemDetail buildProblemDetail(HttpStatus status, String detail, HttpServletRequest request) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(status, detail);
        problemDetail.setTitle(status.getReasonPhrase());
        problemDetail.setInstance(URI.create(request.getRequestURI()));
        problemDetail.setProperty(PROBLEM_TIMESTAMP, LocalDateTime.now().toString());
        problemDetail.setProperty(PROBLEM_ERROR, status.getReasonPhrase());
        problemDetail.setProperty(PROBLEM_MESSAGE, detail);
        problemDetail.setProperty(PROBLEM_PATH, request.getRequestURI());
        return problemDetail;
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ProblemDetail handleInvalidCredentials(InvalidCredentialsException ex, HttpServletRequest request) {
        return buildProblemDetail(HttpStatus.UNAUTHORIZED, ex.getMessage(), request);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ProblemDetail handleResourceNotFound(ResourceNotFoundException ex, HttpServletRequest request) {
        return buildProblemDetail(HttpStatus.NOT_FOUND, ex.getMessage(), request);
    }

    @ExceptionHandler(DuplicateResourceException.class)
    public ProblemDetail handleDuplicateResource(DuplicateResourceException ex, HttpServletRequest request) {
        return buildProblemDetail(HttpStatus.CONFLICT, ex.getMessage(), request);
    }

    @ExceptionHandler({InvalidOperationException.class, BadRequestException.class})
    public ProblemDetail handleBadRequest(RuntimeException ex, HttpServletRequest request) {
        return buildProblemDetail(HttpStatus.BAD_REQUEST, ex.getMessage(), request);
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ProblemDetail handleUnauthorized(UnauthorizedException ex, HttpServletRequest request) {
        return buildProblemDetail(HttpStatus.UNAUTHORIZED, ex.getMessage(), request);
    }

    @ExceptionHandler(UserBannedException.class)
    public ProblemDetail handleUserBanned(UserBannedException ex, HttpServletRequest request) {
        ProblemDetail problemDetail = buildProblemDetail(HttpStatus.FORBIDDEN, ex.getMessage(), request);
        problemDetail.setProperty("code", UserBannedException.CODE);
        problemDetail.setProperty("banReason", ex.getReason());
        problemDetail.setProperty("bannedAt", ex.getBannedAt() != null ? ex.getBannedAt().toString() : null);
        return problemDetail;
    }

    @ExceptionHandler(ForbiddenException.class)
    public ProblemDetail handleForbidden(ForbiddenException ex, HttpServletRequest request) {
        return buildProblemDetail(HttpStatus.FORBIDDEN, ex.getMessage(), request);
    }

    @ExceptionHandler(ExternalServiceException.class)
    public ProblemDetail handleExternalService(ExternalServiceException ex, HttpServletRequest request) {
        return buildProblemDetail(HttpStatus.SERVICE_UNAVAILABLE, ex.getMessage(), request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidationExceptions(MethodArgumentNotValidException ex, HttpServletRequest request) {
        Map<String, String> errors = new LinkedHashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            errors.put(error.getField(), error.getDefaultMessage());
        }

        ProblemDetail problemDetail = buildProblemDetail(HttpStatus.BAD_REQUEST, "Errores de validación", request);
        problemDetail.setTitle("Validation Error");
        problemDetail.setProperty("errors", errors);
        return problemDetail;
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ProblemDetail handleMaxUploadSize(MaxUploadSizeExceededException ex, HttpServletRequest request) {
        return buildProblemDetail(HttpStatus.BAD_REQUEST, "La imagen no debe superar los 2 MB", request);
    }

    @ExceptionHandler(MissingServletRequestPartException.class)
    public ProblemDetail handleMissingPart(MissingServletRequestPartException ex, HttpServletRequest request) {
        return buildProblemDetail(HttpStatus.BAD_REQUEST, "Falta el archivo de la imagen", request);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ProblemDetail handleMessageNotReadable(HttpMessageNotReadableException ex, HttpServletRequest request) {
        return buildProblemDetail(HttpStatus.BAD_REQUEST, "Cuerpo de la petición mal formado", request);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ProblemDetail handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
        return buildProblemDetail(HttpStatus.FORBIDDEN, "No tienes permisos para realizar esta acción", request);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ProblemDetail handleAuthentication(AuthenticationException ex, HttpServletRequest request) {
        return buildProblemDetail(HttpStatus.UNAUTHORIZED, "Credenciales inválidas", request);
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleGlobalException(Exception ex, HttpServletRequest request) {
        log.error("Excepción no controlada en {}", request.getRequestURI(), ex);
        return buildProblemDetail(HttpStatus.INTERNAL_SERVER_ERROR, "Error interno del servidor", request);
    }
}