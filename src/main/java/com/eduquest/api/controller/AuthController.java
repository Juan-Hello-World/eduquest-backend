package com.eduquest.api.controller;

import com.eduquest.api.dto.request.ForgotPasswordRequestDTO;
import com.eduquest.api.dto.request.LoginRequestDTO;
import com.eduquest.api.dto.request.RefreshTokenRequestDTO;
import com.eduquest.api.dto.request.RegisterRequestDTO;
import com.eduquest.api.dto.request.ResetPasswordRequestDTO;
import com.eduquest.api.dto.response.AuthResponseDTO;
import com.eduquest.api.service.AuthService;
import com.eduquest.api.service.PasswordResetService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Autenticación", description = "Registro, login y renovación de token JWT (públicos call)")
public class AuthController {

    private final AuthService authService;
    private final PasswordResetService passwordResetService;

    public AuthController(AuthService authService, PasswordResetService passwordResetService) {
        this.authService = authService;
        this.passwordResetService = passwordResetService;
    }

    @PostMapping("/login")
    @Operation(summary = "Iniciar sesión", description = "Devuelve access token, refresh token y datos del usuario. El access token debe enviarse como 'Authorization: Bearer <token>'.")
    public ResponseEntity<AuthResponseDTO> authenticateUser(@Valid @RequestBody LoginRequestDTO loginRequest) {
        AuthResponseDTO response = authService.authenticateUser(loginRequest);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @PostMapping("/register")
    @Operation(summary = "Registrar usuario", description = "Crea un usuario con rol USER por defecto y devuelve sus tokens JWT.")
    public ResponseEntity<AuthResponseDTO> registerUser(@Valid @RequestBody RegisterRequestDTO registerRequest) {
        AuthResponseDTO response = authService.registerUser(registerRequest);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PostMapping("/refresh")
    @Operation(summary = "Renovar access token", description = "Intercambia un refresh token válido por un nuevo par de tokens.")
    public ResponseEntity<AuthResponseDTO> refreshAccessToken(@Valid @RequestBody RefreshTokenRequestDTO request) {
        AuthResponseDTO response = authService.refreshToken(request.getRefreshToken());
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @PostMapping("/forgot-password")
    @Operation(summary = "Olvidé mi contraseña", description = "Envía al correo un código de 6 dígitos válido por 15 minutos. Responde igual exista o no la cuenta, para no revelar qué correos están registrados.")
    public ResponseEntity<Map<String, String>> forgotPassword(@Valid @RequestBody ForgotPasswordRequestDTO request) {
        passwordResetService.requestReset(request.getEmail());
        return ResponseEntity.accepted().body(Map.of("message",
                "Si el correo está registrado, te enviamos un código para cambiar tu contraseña."));
    }

    @PostMapping("/reset-password")
    @Operation(summary = "Cambiar contraseña con código", description = "Valida el código del correo (5 intentos como máximo) y guarda la nueva contraseña.")
    public ResponseEntity<Void> resetPassword(@Valid @RequestBody ResetPasswordRequestDTO request) {
        passwordResetService.resetPassword(request.getEmail(), request.getCode(), request.getPassword());
        return ResponseEntity.noContent().build();
    }
}
