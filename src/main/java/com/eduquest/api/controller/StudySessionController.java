package com.eduquest.api.controller;

import com.eduquest.api.dto.request.RecordingRequestDTO;
import com.eduquest.api.dto.request.SessionReviewRequestDTO;
import com.eduquest.api.dto.request.StudySessionRequestDTO;
import com.eduquest.api.dto.response.SessionReviewResponseDTO;
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
@Tag(name = "Sesiones de estudio", description = "Agendar, asistir, calificar y adjuntar la grabación de las asesorías de un grupo.")
public class StudySessionController {

    private final StudySessionService studySessionService;

    public StudySessionController(StudySessionService studySessionService) {
        this.studySessionService = studySessionService;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('ROLE_USER') or hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_MANAGER')")
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
    @PreAuthorize("hasAuthority('ROLE_USER') or hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_MANAGER')")
    @Operation(summary = "Listar sesiones de un grupo", description = "Devuelve las sesiones agendadas para el grupo (403 si no es miembro).")
    public ResponseEntity<List<StudySessionResponseDTO>> getSessionsByGroup(
            @PathVariable Long groupId,
            Authentication authentication) {
        String username = authentication.getName();
        List<StudySessionResponseDTO> responses = studySessionService.getSessionsByGroup(groupId, username);
        return new ResponseEntity<>(responses, HttpStatus.OK);
    }

    @PutMapping("/{sessionId}")
    @PreAuthorize("hasAuthority('ROLE_USER') or hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_MANAGER')")
    @Operation(summary = "Editar sesión", description = "Cambia título, fecha o enlace. Solo quien la agendó o el creador del grupo (403 en otro caso).")
    public ResponseEntity<StudySessionResponseDTO> updateSession(
            @PathVariable Long groupId,
            @PathVariable Long sessionId,
            @Valid @RequestBody StudySessionRequestDTO request,
            Authentication authentication) {
        return ResponseEntity.ok(studySessionService.updateSession(groupId, sessionId, request, authentication.getName()));
    }

    @DeleteMapping("/{sessionId}")
    @PreAuthorize("hasAuthority('ROLE_USER') or hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_MANAGER')")
    @Operation(summary = "Eliminar sesión", description = "Solo quien la agendó o el creador del grupo (403 en otro caso).")
    public ResponseEntity<Void> deleteSession(
            @PathVariable Long groupId,
            @PathVariable Long sessionId,
            Authentication authentication) {
        studySessionService.deleteSession(groupId, sessionId, authentication.getName());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{sessionId}/attendance")
    @PreAuthorize("hasAuthority('ROLE_USER') or hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_MANAGER')")
    @Operation(summary = "Confirmar asistencia", description = "Marca que el usuario asistirá. 400 si la sesión ya terminó.")
    public ResponseEntity<StudySessionResponseDTO> attend(
            @PathVariable Long groupId,
            @PathVariable Long sessionId,
            Authentication authentication) {
        return ResponseEntity.ok(studySessionService.setAttendance(groupId, sessionId, authentication.getName(), true));
    }

    @DeleteMapping("/{sessionId}/attendance")
    @PreAuthorize("hasAuthority('ROLE_USER') or hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_MANAGER')")
    @Operation(summary = "Cancelar asistencia")
    public ResponseEntity<StudySessionResponseDTO> cancelAttendance(
            @PathVariable Long groupId,
            @PathVariable Long sessionId,
            Authentication authentication) {
        return ResponseEntity.ok(studySessionService.setAttendance(groupId, sessionId, authentication.getName(), false));
    }

    @GetMapping("/{sessionId}/reviews")
    @PreAuthorize("hasAuthority('ROLE_USER') or hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_MANAGER')")
    @Operation(summary = "Listar reseñas de una asesoría")
    public ResponseEntity<List<SessionReviewResponseDTO>> getReviews(
            @PathVariable Long groupId,
            @PathVariable Long sessionId,
            Authentication authentication) {
        return ResponseEntity.ok(studySessionService.getSessionReviews(groupId, sessionId, authentication.getName()));
    }

    @PostMapping("/{sessionId}/reviews")
    @PreAuthorize("hasAuthority('ROLE_USER') or hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_MANAGER')")
    @Operation(summary = "Calificar asesoría", description = "Solo cuando ya terminó, una vez por usuario y no por quien la agendó.")
    public ResponseEntity<SessionReviewResponseDTO> reviewSession(
            @PathVariable Long groupId,
            @PathVariable Long sessionId,
            @Valid @RequestBody SessionReviewRequestDTO request,
            Authentication authentication) {
        return new ResponseEntity<>(
                studySessionService.reviewSession(groupId, sessionId, request, authentication.getName()),
                HttpStatus.CREATED);
    }

    @PutMapping("/{sessionId}/recording")
    @PreAuthorize("hasAuthority('ROLE_USER') or hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_MANAGER')")
    @Operation(summary = "Adjuntar grabación", description = "Enlace de YouTube validado con oEmbed. Vacío para quitarla. Solo quien agendó o el creador del grupo.")
    public ResponseEntity<StudySessionResponseDTO> updateRecording(
            @PathVariable Long groupId,
            @PathVariable Long sessionId,
            @Valid @RequestBody RecordingRequestDTO request,
            Authentication authentication) {
        return ResponseEntity.ok(studySessionService.updateRecording(groupId, sessionId, request, authentication.getName()));
    }
}
