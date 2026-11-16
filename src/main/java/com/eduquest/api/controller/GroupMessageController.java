package com.eduquest.api.controller;

import com.eduquest.api.dto.request.EditMessageRequestDTO;
import com.eduquest.api.dto.request.GroupMessageRequestDTO;
import com.eduquest.api.dto.request.ReactionRequestDTO;
import com.eduquest.api.dto.response.ChannelSummaryDTO;
import com.eduquest.api.dto.response.GroupMessageResponseDTO;
import com.eduquest.api.dto.response.ThreadResponseDTO;
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
            description = "Sin parámetros devuelve los últimos 50. Con after, los posteriores a ese id (para actualizar). "
                    + "Con before, los 50 anteriores (historial). Con around, los mensajes alrededor de ese id.")
    public ResponseEntity<List<GroupMessageResponseDTO>> getMessages(
            @PathVariable Long groupId,
            @RequestParam(required = false) Long channelId,
            @RequestParam(required = false) Long after,
            @RequestParam(required = false) Long before,
            @RequestParam(required = false) Long around,
            Authentication authentication) {
        return ResponseEntity.ok(messageService.getMessages(groupId, authentication.getName(), channelId, after, before, around));
    }

    @GetMapping("/summary")
    @Operation(summary = "Ponme al día", description = "Resumen con IA de los mensajes que no leíste en el canal.")
    public ResponseEntity<ChannelSummaryDTO> summarize(@PathVariable Long groupId,
                                                       @RequestParam(required = false) Long channelId,
                                                       Authentication authentication) {
        return ResponseEntity.ok(messageService.summarize(groupId, channelId, authentication.getName()));
    }

    @PutMapping("/{messageId}/saved")
    @Operation(summary = "Guardar o quitar de guardados")
    public ResponseEntity<GroupMessageResponseDTO> setSaved(@PathVariable Long groupId,
                                                            @PathVariable Long messageId,
                                                            @RequestParam boolean saved,
                                                            Authentication authentication) {
        return ResponseEntity.ok(messageService.setSaved(groupId, messageId, saved, authentication.getName()));
    }

    @GetMapping("/search")
    @Operation(summary = "Buscar en el historial", description = "Busca texto en los mensajes del grupo; channelId es opcional.")
    public ResponseEntity<List<GroupMessageResponseDTO>> search(@PathVariable Long groupId,
                                                                @RequestParam String q,
                                                                @RequestParam(required = false) Long channelId,
                                                                Authentication authentication) {
        return ResponseEntity.ok(messageService.search(groupId, channelId, q, authentication.getName()));
    }

    @GetMapping("/pins")
    @Operation(summary = "Mensajes fijados del canal")
    public ResponseEntity<List<GroupMessageResponseDTO>> getPinned(@PathVariable Long groupId,
                                                                   @RequestParam(required = false) Long channelId,
                                                                   Authentication authentication) {
        return ResponseEntity.ok(messageService.getPinned(groupId, channelId, authentication.getName()));
    }

    @GetMapping("/{messageId}/thread")
    @Operation(summary = "Ver hilo", description = "Devuelve el mensaje raíz y sus respuestas en orden.")
    public ResponseEntity<ThreadResponseDTO> getThread(@PathVariable Long groupId,
                                                       @PathVariable Long messageId,
                                                       Authentication authentication) {
        return ResponseEntity.ok(messageService.getThread(groupId, messageId, authentication.getName()));
    }

    @PostMapping("/{messageId}/reactions")
    @Operation(summary = "Reaccionar", description = "Agrega la reacción o la quita si ya la tenías.")
    public ResponseEntity<GroupMessageResponseDTO> toggleReaction(@PathVariable Long groupId,
                                                                  @PathVariable Long messageId,
                                                                  @Valid @RequestBody ReactionRequestDTO request,
                                                                  Authentication authentication) {
        return ResponseEntity.ok(messageService.toggleReaction(groupId, messageId, request.getEmoji(),
                authentication.getName()));
    }

    @PutMapping("/{messageId}/pin")
    @Operation(summary = "Fijar o desfijar", description = "Solo el creador o un moderador del grupo.")
    public ResponseEntity<GroupMessageResponseDTO> setPinned(@PathVariable Long groupId,
                                                             @PathVariable Long messageId,
                                                             @RequestParam boolean pinned,
                                                             Authentication authentication) {
        return ResponseEntity.ok(messageService.setPinned(groupId, messageId, pinned, authentication.getName()));
    }

    @PostMapping("/seen")
    @Operation(summary = "Marcar el chat como leído", description = "Reinicia el contador de mensajes sin leer del grupo.")
    public ResponseEntity<Void> markSeen(@PathVariable Long groupId, Authentication authentication) {
        messageService.markChatSeen(groupId, authentication.getName());
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{messageId}")
    @Operation(summary = "Editar mi mensaje")
    public ResponseEntity<GroupMessageResponseDTO> editMessage(@PathVariable Long groupId,
                                                               @PathVariable Long messageId,
                                                               @Valid @RequestBody EditMessageRequestDTO request,
                                                               Authentication authentication) {
        return ResponseEntity.ok(messageService.editMessage(groupId, messageId, request.getContent(),
                authentication.getName()));
    }

    @DeleteMapping("/{messageId}")
    @Operation(summary = "Eliminar mensaje", description = "El autor o un moderador. Queda como «Mensaje eliminado».")
    public ResponseEntity<GroupMessageResponseDTO> deleteMessage(@PathVariable Long groupId,
                                                                 @PathVariable Long messageId,
                                                                 Authentication authentication) {
        return ResponseEntity.ok(messageService.deleteMessage(groupId, messageId, authentication.getName()));
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
