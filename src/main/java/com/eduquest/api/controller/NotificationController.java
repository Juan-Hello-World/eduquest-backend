package com.eduquest.api.controller;

import com.eduquest.api.dto.response.NotificationResponseDTO;
import com.eduquest.api.dto.response.PageResponseDTO;
import com.eduquest.api.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/notifications")
@Tag(name = "Notificaciones", description = "Avisos de grupos, mensajes directos, logros y anuncios del administrador.")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    @Operation(summary = "Mis notificaciones", description = "Paginadas, de la más reciente a la más antigua.")
    public ResponseEntity<PageResponseDTO<NotificationResponseDTO>> getNotifications(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "15") int size,
            Authentication authentication) {
        return ResponseEntity.ok(notificationService.getNotifications(authentication.getName(),
                PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 50))));
    }

    @GetMapping("/unread-count")
    @Operation(summary = "Cantidad sin leer")
    public ResponseEntity<Map<String, Long>> countUnread(Authentication authentication) {
        return ResponseEntity.ok(Map.of("count", notificationService.countUnread(authentication.getName())));
    }

    @PatchMapping("/{notificationId}/read")
    @Operation(summary = "Marcar como leída")
    public ResponseEntity<Void> markRead(@PathVariable Long notificationId, Authentication authentication) {
        notificationService.markRead(notificationId, authentication.getName());
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/read-all")
    @Operation(summary = "Marcar todas como leídas")
    public ResponseEntity<Void> markAllRead(Authentication authentication) {
        notificationService.markAllRead(authentication.getName());
        return ResponseEntity.noContent().build();
    }
}
