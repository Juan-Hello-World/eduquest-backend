package com.eduquest.api.controller;

import com.eduquest.api.dto.request.StudySessionRequestDTO;
import com.eduquest.api.dto.response.StudySessionResponseDTO;
import com.eduquest.api.service.StudySessionService;
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
@RequestMapping("/api/v1/groups/{groupId}/sessions")
@Tag(name = "Sesiones de estudio", description = "Agendar y listar sesiones de estudio asociadas a un grupo.")
public class StudySessionController {

    private final StudySessionService studySessionService;

    public StudySessionController(StudySessionService studySessionService) {
        this.studySessionService = studySessionService;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('ROLE_USER) or hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_MANAGER')")
    @Operation(summary = "Agendar sesión",description = "Crea una sesión de estudio para el grupo indicado (requiere ser miembro).")
    public ResponseEntity<StudySessionResponseDTO> scheduleSession(
            @PathVariable Long groupId,
            @Valid @RequestBody StudySessionRequestDTO request,
            Authentication authentication) {
        String username = authentication.getName();
        StudySessionResponseDTO response = studySessionService.scheduleSession(groupId, request, username);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping
    @PreAuthorize("hasAuthority('ROLE_USER) or hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_MANAGER')")
    @Operation(summary = "Listar sesiones de un grupo", description = "Devuelve las sesiones agendadas para el grupo (403 si no es miembro).")
    public ResponseEntity<List<StudySessionResponseDTO>> getSessionsByGroup(
            @PathVariable Long groupId,
            Authentication authentication) {
        String username = authentication.getName();
        List<StudySessionResponseDTO> responses = studySessionService.getSessionsByGroup(groupId, username);
        return new ResponseEntity<>(responses, HttpStatus.OK);
    }
}