package com.eduquest.api.controller;

import com.eduquest.api.dto.request.StickerRequestDTO;
import com.eduquest.api.dto.response.StickerResponseDTO;
import com.eduquest.api.service.StickerService;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/users/me/stickers")
@PreAuthorize("hasAuthority('ROLE_USER') or hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_MANAGER')")
@Tag(name = "Stickers", description = "Colección personal de stickers para el chat.")
public class StickerController {

    private final StickerService stickerService;

    public StickerController(StickerService stickerService) {
        this.stickerService = stickerService;
    }

    @GetMapping
    @Operation(summary = "Mis stickers")
    public ResponseEntity<List<StickerResponseDTO>> getMyStickers(Authentication authentication) {
        return ResponseEntity.ok(stickerService.getMyStickers(authentication.getName()));
    }

    @PostMapping
    @Operation(summary = "Guardar sticker", description = "La imagen debe subirse antes con /files.")
    public ResponseEntity<StickerResponseDTO> addSticker(@Valid @RequestBody StickerRequestDTO request,
                                                         Authentication authentication) {
        return new ResponseEntity<>(stickerService.addSticker(request.getImageUrl(), authentication.getName()),
                HttpStatus.CREATED);
    }

    @DeleteMapping("/{stickerId}")
    @Operation(summary = "Quitar sticker")
    public ResponseEntity<Void> deleteSticker(@PathVariable Long stickerId, Authentication authentication) {
        stickerService.deleteSticker(stickerId, authentication.getName());
        return ResponseEntity.noContent().build();
    }
}
