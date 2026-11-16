package com.eduquest.api.controller;

import com.eduquest.api.dto.request.EditMessageRequestDTO;
import com.eduquest.api.dto.request.GroupMessageRequestDTO;
import com.eduquest.api.dto.request.RoomRequestDTO;
import com.eduquest.api.dto.request.RoomUpdateRequestDTO;
import com.eduquest.api.dto.response.RoomMessageResponseDTO;
import com.eduquest.api.dto.response.RoomResponseDTO;
import com.eduquest.api.service.ChatRoomService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/rooms")
@PreAuthorize("hasAuthority('ROLE_USER') or hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_MANAGER')")
@Tag(name = "Chats grupales", description = "Conversaciones de 3 a 10 personas fuera de los grupos de estudio.")
public class ChatRoomController {

    private final ChatRoomService roomService;

    public ChatRoomController(ChatRoomService roomService) {
        this.roomService = roomService;
    }

    @GetMapping
    @Operation(summary = "Mis chats grupales")
    public ResponseEntity<List<RoomResponseDTO>> getRooms(Authentication authentication) {
        return ResponseEntity.ok(roomService.getRooms(authentication.getName()));
    }

    @PostMapping
    @Operation(summary = "Crear chat grupal")
    public ResponseEntity<RoomResponseDTO> createRoom(@Valid @RequestBody RoomRequestDTO request, Authentication authentication) {
        return new ResponseEntity<>(roomService.createRoom(request, authentication.getName()), HttpStatus.CREATED);
    }

    @GetMapping("/{roomId}")
    @Operation(summary = "Detalle del chat grupal")
    public ResponseEntity<RoomResponseDTO> getRoom(@PathVariable Long roomId, Authentication authentication) {
        return ResponseEntity.ok(roomService.getRoom(roomId, authentication.getName()));
    }

    @PatchMapping("/{roomId}")
    @Operation(summary = "Renombrar o agregar personas")
    public ResponseEntity<RoomResponseDTO> updateRoom(@PathVariable Long roomId, @Valid @RequestBody RoomUpdateRequestDTO request,
                                                      Authentication authentication) {
        return ResponseEntity.ok(roomService.updateRoom(roomId, request, authentication.getName()));
    }

    @PostMapping("/{roomId}/leave")
    @Operation(summary = "Salir del chat", description = "Cuando sale la última persona el chat se elimina.")
    public ResponseEntity<Void> leaveRoom(@PathVariable Long roomId, Authentication authentication) {
        roomService.leaveRoom(roomId, authentication.getName());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{roomId}/messages")
    @Operation(summary = "Mensajes del chat grupal")
    public ResponseEntity<List<RoomMessageResponseDTO>> getMessages(@PathVariable Long roomId,
                                                                    @RequestParam(required = false) Long after,
                                                                    @RequestParam(required = false) Long before,
                                                                    Authentication authentication) {
        return ResponseEntity.ok(roomService.getMessages(roomId, authentication.getName(), after, before));
    }

    @PostMapping("/{roomId}/messages")
    @Operation(summary = "Enviar mensaje")
    public ResponseEntity<RoomMessageResponseDTO> sendMessage(@PathVariable Long roomId,
                                                              @Valid @RequestBody GroupMessageRequestDTO request,
                                                              Authentication authentication) {
        return new ResponseEntity<>(roomService.sendMessage(roomId, request, authentication.getName()), HttpStatus.CREATED);
    }

    @PutMapping("/{roomId}/messages/{messageId}")
    @Operation(summary = "Editar mi mensaje")
    public ResponseEntity<RoomMessageResponseDTO> editMessage(@PathVariable Long roomId, @PathVariable Long messageId,
                                                              @Valid @RequestBody EditMessageRequestDTO request,
                                                              Authentication authentication) {
        return ResponseEntity.ok(roomService.editMessage(roomId, messageId, request.getContent(), authentication.getName()));
    }

    @DeleteMapping("/{roomId}/messages/{messageId}")
    @Operation(summary = "Eliminar mi mensaje")
    public ResponseEntity<RoomMessageResponseDTO> deleteMessage(@PathVariable Long roomId, @PathVariable Long messageId,
                                                                Authentication authentication) {
        return ResponseEntity.ok(roomService.deleteMessage(roomId, messageId, authentication.getName()));
    }

    @PostMapping("/{roomId}/read")
    @Operation(summary = "Marcar como leído")
    public ResponseEntity<Void> markRead(@PathVariable Long roomId, Authentication authentication) {
        roomService.markRead(roomId, authentication.getName());
        return ResponseEntity.noContent().build();
    }
}
