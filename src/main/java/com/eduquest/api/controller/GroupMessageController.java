package com.eduquest.api.controller;

import com.eduquest.api.dto.request.GroupMessageRequestDTO;
import com.eduquest.api.dto.response.GroupMessageResponseDTO;
import com.eduquest.api.service.GroupMessageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/groups/{groupId}/messages")
@PreAuthorize("hasAuthority('ROLE_USER') or hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_MANAGER')")
@Tag(name = "Chat de grupo", description = "Mensajes entre los miembros de un grupo.")
public class GroupMessageController {

    private final GroupMessageService messageService;

    public GroupMessageController(GroupMessageService messageService) {
        this.messageService = messageService;
    }

    @GetMapping
    @Operation(summary = "Listar mensajes",
            description = "Sin parámetros devuelve los últimos 50. Con after, los posteriores a ese id (para actualizar). Con before, los 50 anteriores (historial).")
    public ResponseEntity<List<GroupMessageResponseDTO>> getMessages(
            @PathVariable Long groupId,
            @RequestParam(required = false) Long after,
            @RequestParam(required = false) Long before,
            Authentication authentication) {
        return ResponseEntity.ok(messageService.getMessages(groupId, authentication.getName(), after, before));
    }

    @PostMapping("/seen")
    @Operation(summary = "Marcar el chat como leído", description = "Reinicia el contador de mensajes sin leer del grupo.")
    public ResponseEntity<Void> markSeen(@PathVariable Long groupId, Authentication authentication) {
        messageService.markChatSeen(groupId, authentication.getName());
        return ResponseEntity.noContent().build();
    }

    @PostMapping
    @Operation(summary = "Enviar mensaje")
    public ResponseEntity<GroupMessageResponseDTO> sendMessage(
            @PathVariable Long groupId,
            @Valid @RequestBody GroupMessageRequestDTO request,
            Authentication authentication) {
        return new ResponseEntity<>(messageService.sendMessage(groupId, request, authentication.getName()),
                HttpStatus.CREATED);
    }
}
