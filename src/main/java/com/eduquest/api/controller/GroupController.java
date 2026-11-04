package com.eduquest.api.controller;

import com.eduquest.api.dto.request.DocumentRequestDTO;
import com.eduquest.api.dto.request.GroupRequestDTO;
import com.eduquest.api.dto.response.DocumentResponseDTO;
import com.eduquest.api.dto.response.GroupResponseDTO;
import com.eduquest.api.dto.response.PageResponseDTO;
import com.eduquest.api.service.DocumentService;
import com.eduquest.api.service.GroupService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/groups")
@Tag(name = "Grupos de estudio", description = "Creación y consulta de grupos; subida de documentos solo para miembros del grupo.")
public class GroupController {

    private final GroupService groupService;
    private final DocumentService documentService;

    public GroupController(GroupService groupService, DocumentService documentService) {
        this.groupService = groupService;
        this.documentService = documentService;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('ROLE_USER') or hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_MANAGER')")
    @Operation(summary = "Crear grupo", description = "Crea un grupo: el creador se une automáticamente como miembro.")
    public ResponseEntity<GroupResponseDTO> createGroup(
            @Valid @RequestBody GroupRequestDTO request,
            Authentication authentication) {
        String username = authentication.getName();
        GroupResponseDTO response = groupService.createGroup(request, username);
        return new ResponseEntity<GroupResponseDTO>(response, HttpStatus.CREATED);
    }

    @GetMapping
    @Operation(summary = "Listar grupos", description = "Devuelve grupos paginados.")
    public ResponseEntity<PageResponseDTO<GroupResponseDTO>> getAllGroups(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100));
        return new ResponseEntity<PageResponseDTO<GroupResponseDTO>>(groupService.getAllGroups(pageable), HttpStatus.OK);
    }

    @PostMapping("/{groupId}/documents")
    @PreAuthorize("hasAuthority('ROLE_USER') or hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_MANAGER')")
    @Operation(summary = "Subir documento a un grupo", description = "403 si el usuario autenticado no es miembro del grupo.")
    public ResponseEntity<DocumentResponseDTO> uploadDocumentToGroup(
            @PathVariable Long groupId,
            @Valid @RequestBody DocumentRequestDTO request,
            Authentication authentication) {
        String username = authentication.getName();
        DocumentResponseDTO response = documentService.uploadDocumentToGroup(groupId, request, username);
        return new ResponseEntity<DocumentResponseDTO>(response, HttpStatus.CREATED);
    }

    @PostMapping("/{groupId}/join")
    @PreAuthorize("hasAuthority('ROLE_USER') or hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_MANAGER')")
    @Operation(summary = "Unirse a un grupo", description = "Agrega al usuario autenticado como miembro del grupo (409 si ya es miembro).")
    public ResponseEntity<GroupResponseDTO> joinGroup(
            @PathVariable Long groupId,
            Authentication authentication) {
        String username = authentication.getName();
        return ResponseEntity.ok(groupService.joinGroup(groupId, username));
    }

    @GetMapping("/{groupId}/documents")
    @PreAuthorize("hasAuthority('ROLE_USER') or hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_MANAGER')")
    @Operation(summary = "Listar documentos de un grupo", description = "Devuelve los documentos paginados del grupo. 403 si no es miembro.")
    public ResponseEntity<PageResponseDTO<DocumentResponseDTO>> getGroupDocuments(
            @PathVariable Long groupId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Authentication authentication) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100));
        String username = authentication.getName();
        return ResponseEntity.ok(documentService.getDocumentsByGroup(groupId, username, pageable));
    }
}