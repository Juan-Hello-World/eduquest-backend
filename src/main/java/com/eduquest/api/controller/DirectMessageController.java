package com.eduquest.api.controller;

import com.eduquest.api.dto.request.GroupMessageRequestDTO;
import com.eduquest.api.dto.response.ConversationResponseDTO;
import com.eduquest.api.dto.response.DirectMessageResponseDTO;
import com.eduquest.api.dto.response.UserSummaryDTO;
import com.eduquest.api.service.DirectMessageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/conversations")
@Tag(name = "Mensajes directos", description = "Chat privado entre dos usuarios.")
public class DirectMessageController {

    private final DirectMessageService messageService;

    public DirectMessageController(DirectMessageService messageService) {
        this.messageService = messageService;
    }

    @GetMapping
    @Operation(summary = "Mis conversaciones", description = "Una por persona, con el último mensaje y los no leídos. La más reciente primero.")
    public ResponseEntity<List<ConversationResponseDTO>> getConversations(Authentication authentication) {
        return ResponseEntity.ok(messageService.getConversations(authentication.getName()));
    }

    @GetMapping("/unread-count")
    @Operation(summary = "Mensajes directos sin leer")
    public ResponseEntity<Map<String, Long>> countUnread(Authentication authentication) {
        return ResponseEntity.ok(Map.of("count", messageService.countUnread(authentication.getName())));
    }

    @GetMapping("/contacts")
    @Operation(summary = "Buscar usuarios", description = "Por nombre de usuario (mínimo 2 caracteres), hasta 10 resultados.")
    public ResponseEntity<List<UserSummaryDTO>> searchUsers(@RequestParam(defaultValue = "") String search,
                                                            Authentication authentication) {
        return ResponseEntity.ok(messageService.searchUsers(authentication.getName(), search));
    }

    @GetMapping("/contacts/{userId}")
    @Operation(summary = "Datos públicos de un usuario")
    public ResponseEntity<UserSummaryDTO> getUser(@PathVariable Long userId) {
        return ResponseEntity.ok(messageService.getUserSummary(userId));
    }

    @GetMapping("/{userId}/messages")
    @Operation(summary = "Mensajes con un usuario", description = "Sin parámetros, los últimos 50. after para los nuevos y before para el historial.")
    public ResponseEntity<List<DirectMessageResponseDTO>> getMessages(@PathVariable Long userId,
                                                                      @RequestParam(required = false) Long after,
                                                                      @RequestParam(required = false) Long before,
                                                                      Authentication authentication) {
        return ResponseEntity.ok(messageService.getMessages(authentication.getName(), userId, after, before));
    }

    @PostMapping("/{userId}/messages")
    @Operation(summary = "Enviar mensaje directo")
    public ResponseEntity<DirectMessageResponseDTO> sendMessage(@PathVariable Long userId,
                                                                @Valid @RequestBody GroupMessageRequestDTO request,
                                                                Authentication authentication) {
        return new ResponseEntity<>(messageService.sendMessage(authentication.getName(), userId, request.getContent()),
                HttpStatus.CREATED);
    }

    @PostMapping("/{userId}/read")
    @Operation(summary = "Marcar la conversación como leída")
    public ResponseEntity<Void> markRead(@PathVariable Long userId, Authentication authentication) {
        messageService.markRead(authentication.getName(), userId);
        return ResponseEntity.noContent().build();
    }
}
