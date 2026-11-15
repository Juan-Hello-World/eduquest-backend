package com.eduquest.api.controller;

import com.eduquest.api.dto.request.DocumentRequestDTO;
import com.eduquest.api.dto.request.GroupRequestDTO;
import com.eduquest.api.dto.request.GroupPasswordRequestDTO;
import com.eduquest.api.dto.request.JoinGroupRequestDTO;
import com.eduquest.api.dto.response.DocumentResponseDTO;
import com.eduquest.api.dto.response.GroupInviteResponseDTO;
import com.eduquest.api.dto.response.GroupMemberResponseDTO;
import com.eduquest.api.dto.response.GroupResponseDTO;
import com.eduquest.api.dto.response.PageResponseDTO;
import com.eduquest.api.service.DocumentService;
import com.eduquest.api.service.GroupService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
    @Operation(summary = "Listar grupos", description = "Grupos públicos y los privados a los que pertenece el usuario, paginados. search filtra por nombre.")
    public ResponseEntity<PageResponseDTO<GroupResponseDTO>> getAllGroups(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String search,
            Authentication authentication) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100));
        return new ResponseEntity<PageResponseDTO<GroupResponseDTO>>(
                groupService.getAllGroups(pageable, authentication.getName(), search), HttpStatus.OK);
    }

    @GetMapping("/mine")
    @Operation(summary = "Mis grupos", description = "Grupos del usuario ordenados por última actividad, con la cantidad de documentos y sesiones nuevas desde su última visita.")
    public ResponseEntity<List<GroupResponseDTO>> getMyGroups(Authentication authentication) {
        return ResponseEntity.ok(groupService.getMyGroups(authentication.getName()));
    }

    @PostMapping("/{groupId}/seen")
    @PreAuthorize("hasAuthority('ROLE_USER') or hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_MANAGER')")
    @Operation(summary = "Marcar grupo como visto", description = "Deja en cero las novedades del grupo para el usuario. 403 si no es miembro.")
    public ResponseEntity<Void> markAsSeen(@PathVariable Long groupId, Authentication authentication) {
        groupService.markAsSeen(groupId, authentication.getName());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/code/{code}")
    @Operation(summary = "Buscar grupo por código", description = "Encuentra un grupo público o privado por su código único. 404 si no existe.")
    public ResponseEntity<GroupResponseDTO> getGroupByCode(@PathVariable String code, Authentication authentication) {
        return ResponseEntity.ok(groupService.getGroupByCode(code, authentication.getName()));
    }

    @GetMapping("/{groupId}")
    @Operation(summary = "Obtener grupo por id", description = "Los grupos privados solo son visibles para sus miembros (404 en otro caso).")
    public ResponseEntity<GroupResponseDTO> getGroupById(@PathVariable Long groupId, Authentication authentication) {
        return ResponseEntity.ok(groupService.getGroupById(groupId, authentication.getName()));
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
    @Operation(summary = "Unirse a un grupo", description = "Los privados piden la contraseña del grupo o un inviteToken vigente (403 si no son válidos). 409 si ya es miembro.")
    public ResponseEntity<GroupResponseDTO> joinGroup(
            @PathVariable Long groupId,
            @RequestBody(required = false) JoinGroupRequestDTO request,
            Authentication authentication) {
        String password = request != null ? request.getPassword() : null;
        String inviteToken = request != null ? request.getInviteToken() : null;
        return ResponseEntity.ok(groupService.joinGroup(groupId, authentication.getName(), password, inviteToken));
    }

    @PostMapping("/{groupId}/invite")
    @PreAuthorize("hasAuthority('ROLE_USER') or hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_MANAGER')")
    @Operation(summary = "Enlace de invitación", description = "Solo el creador. Devuelve el enlace vigente (7 días) o crea uno; con regenerate=true invalida el anterior. Quien lo usa entra sin contraseña.")
    public ResponseEntity<GroupInviteResponseDTO> createInvite(
            @PathVariable Long groupId,
            @RequestParam(defaultValue = "false") boolean regenerate,
            Authentication authentication) {
        return ResponseEntity.ok(groupService.createInvite(groupId, authentication.getName(), regenerate));
    }

    @PutMapping("/{groupId}/password")
    @PreAuthorize("hasAuthority('ROLE_USER') or hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_MANAGER')")
    @Operation(summary = "Cambiar contraseña del grupo", description = "Solo el creador de un grupo privado. La contraseña se guarda cifrada, por eso no se puede consultar.")
    public ResponseEntity<Void> changePassword(
            @PathVariable Long groupId,
            @Valid @RequestBody GroupPasswordRequestDTO request,
            Authentication authentication) {
        groupService.changePassword(groupId, request.getPassword(), authentication.getName());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{groupId}/members")
    @PreAuthorize("hasAuthority('ROLE_USER') or hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_MANAGER')")
    @Operation(summary = "Listar integrantes de un grupo", description = "Creador primero y luego por nombre. 403 si no es miembro.")
    public ResponseEntity<List<GroupMemberResponseDTO>> getGroupMembers(
            @PathVariable Long groupId,
            Authentication authentication) {
        return ResponseEntity.ok(groupService.getMembers(groupId, authentication.getName()));
    }

    @GetMapping("/{groupId}/documents")
    @PreAuthorize("hasAuthority('ROLE_USER') or hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_MANAGER')")
    @Operation(summary = "Listar documentos de un grupo", description = "Devuelve los documentos paginados del grupo. 403 si no es miembro.")
    public ResponseEntity<PageResponseDTO<DocumentResponseDTO>> getGroupDocuments(
            @PathVariable Long groupId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Authentication authentication) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100),
                Sort.by(Sort.Direction.DESC, "uploadedAt"));
        String username = authentication.getName();
        return ResponseEntity.ok(documentService.getDocumentsByGroup(groupId, username, pageable));
    }
}