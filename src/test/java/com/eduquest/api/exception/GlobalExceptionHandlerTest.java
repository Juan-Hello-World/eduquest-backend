package com.eduquest.api.exception;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.core.MethodParameter;
import org.springframework.mock.http.MockHttpInputMessage;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;

import jakarta.servlet.http.HttpServletRequest;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
    }

    private HttpServletRequest request() {
        return new MockHttpServletRequest("GET", "/api/v1/documents");
    }

    @Test
    void handleResourceNotFound_returns404ProblemDetail() {
        ProblemDetail detail = handler.handleResourceNotFound(
                new ResourceNotFoundException("Document not found: 1"), request());

        assertThat(detail.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
        assertThat(detail.getTitle()).isEqualTo("Not Found");
        assertThat(detail.getProperties().get("error")).isNotNull();
        assertThat(detail.getProperties().get("message")).isEqualTo("Document not found: 1");
        assertThat(detail.getProperties().get("timestamp")).isNotNull();
        assertThat(detail.getProperties().get("path")).isEqualTo("/api/v1/documents");
    }

    @Test
    void handleDuplicateResource_returns409ProblemDetail() {
        ProblemDetail detail = handler.handleDuplicateResource(
                new DuplicateResourceException("El usuario ya existe"), request());

        assertThat(detail.getStatus()).isEqualTo(HttpStatus.CONFLICT.value());
        assertThat(detail.getProperties().get("message")).isEqualTo("El usuario ya existe");
    }

    @Test
    void handleBadRequest_forBadRequestException_returns400() {
        ProblemDetail detail = handler.handleBadRequest(
                new BadRequestException("Datos invalidos"), request());

        assertThat(detail.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(detail.getProperties().get("message")).isEqualTo("Datos invalidos");
    }

    @Test
    void handleBadRequest_forInvalidOperationException_returns400() {
        ProblemDetail detail = handler.handleBadRequest(
                new InvalidOperationException("Operacion invalida"), request());

        assertThat(detail.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
    }

    @Test
    void handleInvalidCredentials_returns401ProblemDetail() {
        ProblemDetail detail = handler.handleInvalidCredentials(
                new InvalidCredentialsException("Usuario o contraseña incorrectos"), request());

        assertThat(detail.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
        assertThat(detail.getProperties().get("message")).isEqualTo("Usuario o contraseña incorrectos");
    }

    @Test
    void handleUnauthorized_returns401ProblemDetail() {
        ProblemDetail detail = handler.handleUnauthorized(
                new UnauthorizedException("Refresh token inválido"), request());

        assertThat(detail.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
    }

    @Test
    void handleForbidden_returns403ProblemDetail() {
        ProblemDetail detail = handler.handleForbidden(
                new ForbiddenException("No autorizado"), request());

        assertThat(detail.getStatus()).isEqualTo(HttpStatus.FORBIDDEN.value());
    }

    @Test
    void handleExternalService_returns500ProblemDetail() {
        ProblemDetail detail = handler.handleExternalService(
                new ExternalServiceException("OpenAI caido"), request());

        assertThat(detail.getStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.value());
    }

    @Test
    void handleValidationExceptions_returns400WithFieldErrors() throws Exception {
        MethodParameter parameter = new MethodParameter(
                SampleBean.class.getDeclaredMethod("method", String.class), 0);
        BeanPropertyBindingResult binding = new BeanPropertyBindingResult(new SampleBean(), "request");
        binding.rejectValue("username", "NotEmpty", "el nombre es requerido");

        ProblemDetail detail = handler.handleValidationExceptions(
                new MethodArgumentNotValidException(parameter, binding), request());

        assertThat(detail.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(detail.getTitle()).isEqualTo("Validation Error");
        assertThat((Map<String, String>) detail.getProperties().get("errors"))
                .containsEntry("username", "el nombre es requerido");
    }

    @Test
    void handleMessageNotReadable_returns400ProblemDetail() {
        ProblemDetail detail = handler.handleMessageNotReadable(
                new HttpMessageNotReadableException("malformed",
                        new MockHttpInputMessage(new byte[0])),
                request());

        assertThat(detail.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
    }

    @Test
    void handleAccessDenied_returns403ProblemDetail() {
        ProblemDetail detail = handler.handleAccessDenied(
                new AccessDeniedException("denied"), request());

        assertThat(detail.getStatus()).isEqualTo(HttpStatus.FORBIDDEN.value());
    }

    @Test
    void handleAuthentication_returns401ProblemDetail() {
        ProblemDetail detail = handler.handleAuthentication(
                new BadCredentialsException("bad"), request());

        assertThat(detail.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
    }

    @Test
    void handleGlobalException_returns500ProblemDetail() {
        ProblemDetail detail = handler.handleGlobalException(new RuntimeException("boom"), request());

        assertThat(detail.getStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.value());
        assertThat(detail.getProperties().get("message")).isEqualTo("Error interno del servidor");
    }

    static class SampleBean {
        private String username;

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public void method(String username) {
        }
    }
}