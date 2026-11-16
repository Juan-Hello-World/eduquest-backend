package com.eduquest.api.controller;

import com.eduquest.api.dto.request.PinnedChatRequestDTO;
import com.eduquest.api.dto.request.StatusRequestDTO;
import com.eduquest.api.dto.response.PinnedChatDTO;
import com.eduquest.api.dto.response.UserStatusDTO;
import com.eduquest.api.service.PersonalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/users")
@PreAuthorize("hasAuthority('ROLE_USER') or hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_MANAGER')")
@Tag(name = "Estado y chats fijados", description = "Estado personalizado, disponibilidad y chats fijados.")
public class PersonalController {

    private final PersonalService personalService;

    public PersonalController(PersonalService personalService) {
        this.personalService = personalService;
    }

    @PutMapping("/me/status")
    @Operation(summary = "Cambiar mi estado", description = "Disponibilidad (AVAILABLE, BUSY, AWAY, INVISIBLE), texto y emoji.")
    public ResponseEntity<UserStatusDTO> updateStatus(@Valid @RequestBody StatusRequestDTO request,
                                                      Authentication authentication) {
        return ResponseEntity.ok(personalService.updateStatus(authentication.getName(), request));
    }

    @GetMapping("/statuses")
    @Operation(summary = "Estado de varias personas", description = "ids separados por coma, hasta 100.")
    public ResponseEntity<List<UserStatusDTO>> getStatuses(@RequestParam List<Long> ids) {
        return ResponseEntity.ok(personalService.getStatuses(ids));
    }

    @GetMapping("/me/pinned-chats")
    @Operation(summary = "Mis chats fijados")
    public ResponseEntity<List<PinnedChatDTO>> getPinnedChats(Authentication authentication) {
        return ResponseEntity.ok(personalService.getPinnedChats(authentication.getName()));
    }

    @PutMapping("/me/pinned-chats")
    @Operation(summary = "Fijar o desfijar un chat", description = "kind: GROUP, DIRECT o ROOM.")
    public ResponseEntity<List<PinnedChatDTO>> setPinned(@Valid @RequestBody PinnedChatRequestDTO request,
                                                         Authentication authentication) {
        return ResponseEntity.ok(personalService.setPinned(authentication.getName(), request));
    }
}
