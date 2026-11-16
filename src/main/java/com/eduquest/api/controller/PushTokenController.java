package com.eduquest.api.controller;

import com.eduquest.api.dto.request.PushTokenRequestDTO;
import com.eduquest.api.service.PushNotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users/me/push-tokens")
@Tag(name = "Usuarios")
public class PushTokenController {

    private final PushNotificationService pushService;

    public PushTokenController(PushNotificationService pushService) {
        this.pushService = pushService;
    }

    @PostMapping
    @Operation(summary = "Registrar este celular para notificaciones push", description = "Token de Expo (ExponentPushToken[...]).")
    public ResponseEntity<Void> register(@Valid @RequestBody PushTokenRequestDTO request, Authentication authentication) {
        pushService.register(authentication.getName(), request.getToken());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping
    @Operation(summary = "Dejar de recibir push en este celular", description = "Se llama al cerrar sesión.")
    public ResponseEntity<Void> unregister(@Valid @RequestBody PushTokenRequestDTO request, Authentication authentication) {
        pushService.unregister(authentication.getName(), request.getToken());
        return ResponseEntity.noContent().build();
    }
}
