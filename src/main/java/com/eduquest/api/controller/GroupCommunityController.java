package com.eduquest.api.controller;

import com.eduquest.api.dto.request.ChannelRequestDTO;
import com.eduquest.api.dto.request.ModerationRequestDTO;
import com.eduquest.api.dto.response.ChannelResponseDTO;
import com.eduquest.api.dto.response.ModerationResponseDTO;
import com.eduquest.api.dto.response.VoiceRoomDTO;
import com.eduquest.api.service.GroupCommunityService;
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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/groups/{groupId}")
@PreAuthorize("hasAuthority('ROLE_USER') or hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_MANAGER')")
@Tag(name = "Comunidad del grupo", description = "Canales, moderadores, silencio, sala de voz y presencia.")
public class GroupCommunityController {

    private final GroupCommunityService communityService;

    public GroupCommunityController(GroupCommunityService communityService) {
        this.communityService = communityService;
    }

    @GetMapping("/channels")
    @Operation(summary = "Listar canales", description = "Si el grupo no tiene canales se crea #general.")
    public ResponseEntity<List<ChannelResponseDTO>> getChannels(@PathVariable Long groupId,
                                                                Authentication authentication) {
        return ResponseEntity.ok(communityService.getChannels(groupId, authentication.getName()));
    }

    @PostMapping("/channels")
    @Operation(summary = "Crear canal", description = "Solo el creador o un moderador del grupo.")
    public ResponseEntity<ChannelResponseDTO> createChannel(@PathVariable Long groupId,
                                                            @Valid @RequestBody ChannelRequestDTO request,
                                                            Authentication authentication) {
        return new ResponseEntity<>(communityService.createChannel(groupId, request, authentication.getName()),
                HttpStatus.CREATED);
    }

    @PutMapping("/channels/{channelId}")
    @Operation(summary = "Editar canal")
    public ResponseEntity<ChannelResponseDTO> updateChannel(@PathVariable Long groupId,
                                                            @PathVariable Long channelId,
                                                            @Valid @RequestBody ChannelRequestDTO request,
                                                            Authentication authentication) {
        return ResponseEntity.ok(communityService.updateChannel(groupId, channelId, request, authentication.getName()));
    }

    @DeleteMapping("/channels/{channelId}")
    @Operation(summary = "Eliminar canal", description = "Borra también sus mensajes. El grupo conserva al menos un canal.")
    public ResponseEntity<Void> deleteChannel(@PathVariable Long groupId,
                                              @PathVariable Long channelId,
                                              Authentication authentication) {
        communityService.deleteChannel(groupId, channelId, authentication.getName());
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/members/{memberId}/moderator")
    @Operation(summary = "Nombrar o quitar moderador", description = "Solo el creador del grupo.")
    public ResponseEntity<Void> setModerator(@PathVariable Long groupId,
                                             @PathVariable Long memberId,
                                             @RequestParam boolean moderator,
                                             Authentication authentication) {
        communityService.setModerator(groupId, memberId, moderator, authentication.getName());
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/mute")
    @Operation(summary = "Silenciar el grupo", description = "Deja de recibir notificaciones de actividad del grupo.")
    public ResponseEntity<Void> setMuted(@PathVariable Long groupId,
                                         @RequestParam boolean muted,
                                         Authentication authentication) {
        communityService.setMuted(groupId, muted, authentication.getName());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/voice-room")
    @Operation(summary = "Sala de voz del grupo", description = "Enlace fijo de videollamada para los miembros.")
    public ResponseEntity<Map<String, String>> getVoiceRoom(@PathVariable Long groupId,
                                                            Authentication authentication) {
        return ResponseEntity.ok(Map.of("url", communityService.getVoiceRoomUrl(groupId, authentication.getName())));
    }

    @PostMapping("/channels/{channelId}/read")
    @Operation(summary = "Marcar canal como leído", description = "Reinicia los no leídos y las menciones del canal.")
    public ResponseEntity<Void> markChannelRead(@PathVariable Long groupId,
                                                @PathVariable Long channelId,
                                                Authentication authentication) {
        communityService.markChannelRead(groupId, channelId, authentication.getName());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/moderation")
    @Operation(summary = "Palabras prohibidas del grupo")
    public ResponseEntity<ModerationResponseDTO> getModeration(@PathVariable Long groupId, Authentication authentication) {
        return ResponseEntity.ok(communityService.getModeration(groupId, authentication.getName()));
    }

    @PutMapping("/moderation")
    @Operation(summary = "Actualizar palabras prohibidas", description = "Solo el creador o un moderador. Hasta 50 palabras.")
    public ResponseEntity<ModerationResponseDTO> updateModeration(@PathVariable Long groupId,
                                                                  @Valid @RequestBody ModerationRequestDTO request,
                                                                  Authentication authentication) {
        return ResponseEntity.ok(communityService.updateModeration(groupId, request.getBannedWords(),
                authentication.getName()));
    }

    @GetMapping("/voice")
    @Operation(summary = "Quién está en la sala de voz")
    public ResponseEntity<VoiceRoomDTO> getVoice(@PathVariable Long groupId, Authentication authentication) {
        return ResponseEntity.ok(communityService.getVoice(groupId, authentication.getName()));
    }

    @PostMapping("/voice/join")
    @Operation(summary = "Entrar a la sala de voz", description = "Devuelve el enlace y avisa a los demás miembros.")
    public ResponseEntity<VoiceRoomDTO> joinVoice(@PathVariable Long groupId, Authentication authentication) {
        return ResponseEntity.ok(communityService.setInVoice(groupId, true, authentication.getName()));
    }

    @PostMapping("/voice/leave")
    @Operation(summary = "Salir de la sala de voz")
    public ResponseEntity<VoiceRoomDTO> leaveVoice(@PathVariable Long groupId, Authentication authentication) {
        return ResponseEntity.ok(communityService.setInVoice(groupId, false, authentication.getName()));
    }

    @GetMapping("/online")
    @Operation(summary = "Miembros conectados")
    public ResponseEntity<Map<String, List<Long>>> getOnline(@PathVariable Long groupId,
                                                             Authentication authentication) {
        return ResponseEntity.ok(Map.of("userIds", communityService.getOnlineMembers(groupId, authentication.getName())));
    }
}
