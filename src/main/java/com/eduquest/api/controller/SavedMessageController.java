package com.eduquest.api.controller;

import com.eduquest.api.dto.response.SavedMessageDTO;
import com.eduquest.api.service.GroupMessageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/users/me/saved-messages")
@PreAuthorize("hasAuthority('ROLE_USER') or hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_MANAGER')")
@Tag(name = "Mensajes guardados", description = "Mensajes del chat de grupo guardados para leer después.")
public class SavedMessageController {

    private final GroupMessageService messageService;

    public SavedMessageController(GroupMessageService messageService) {
        this.messageService = messageService;
    }

    @GetMapping
    @Operation(summary = "Mis mensajes guardados")
    public ResponseEntity<List<SavedMessageDTO>> getSavedMessages(Authentication authentication) {
        return ResponseEntity.ok(messageService.getSavedMessages(authentication.getName()));
    }
}
