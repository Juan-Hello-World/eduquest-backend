package com.eduquest.api.controller;

import com.eduquest.api.dto.request.VerifyEmailRequestDTO;
import com.eduquest.api.dto.response.UserResponseDTO;
import com.eduquest.api.security.UserPrincipal;
import com.eduquest.api.service.EmailVerificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users/me/verify-email")
@Tag(name = "Usuarios")
public class EmailVerificationController {

    private final EmailVerificationService verificationService;

    public EmailVerificationController(EmailVerificationService verificationService) {
        this.verificationService = verificationService;
    }

    @PostMapping
    @Operation(summary = "Verificar mi correo", description = "Código de 6 dígitos enviado al registrarse. Hasta verificarlo la cuenta es de solo lectura.")
    public ResponseEntity<UserResponseDTO> verify(@Valid @RequestBody VerifyEmailRequestDTO request,
                                                  @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(verificationService.verify(principal.getUsername(), request.getCode()));
    }

    @PostMapping("/resend")
    @Operation(summary = "Reenviar el código", description = "Una vez por minuto.")
    public ResponseEntity<Void> resend(@AuthenticationPrincipal UserPrincipal principal) {
        verificationService.resend(principal.getUsername());
        return ResponseEntity.noContent().build();
    }
}
