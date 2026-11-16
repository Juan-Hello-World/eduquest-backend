package com.eduquest.api.controller;

import com.eduquest.api.dto.request.StudyRoomRequestDTO;
import com.eduquest.api.dto.response.StudyRoomDTO;
import com.eduquest.api.service.StudyRoomService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/groups/{groupId}/study-room")
@PreAuthorize("hasAuthority('ROLE_USER') or hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_MANAGER')")
@Tag(name = "Sala de estudio", description = "Pomodoro compartido y sincronizado entre los miembros del grupo.")
public class StudyRoomController {

    private final StudyRoomService studyRoomService;

    public StudyRoomController(StudyRoomService studyRoomService) {
        this.studyRoomService = studyRoomService;
    }

    @GetMapping
    @Operation(summary = "Estado de la sala", description = "La fase actual se calcula con startedAt, focusMinutes y breakMinutes.")
    public ResponseEntity<StudyRoomDTO> get(@PathVariable Long groupId, Authentication authentication) {
        return ResponseEntity.ok(studyRoomService.get(groupId, authentication.getName()));
    }

    @PostMapping("/start")
    @Operation(summary = "Iniciar sala de estudio")
    public ResponseEntity<StudyRoomDTO> start(@PathVariable Long groupId, @Valid @RequestBody StudyRoomRequestDTO request,
                                              Authentication authentication) {
        return ResponseEntity.ok(studyRoomService.start(groupId, request, authentication.getName()));
    }

    @PostMapping("/join")
    @Operation(summary = "Unirme a la sala")
    public ResponseEntity<StudyRoomDTO> join(@PathVariable Long groupId, Authentication authentication) {
        return ResponseEntity.ok(studyRoomService.join(groupId, authentication.getName()));
    }

    @PostMapping("/leave")
    @Operation(summary = "Salir de la sala", description = "Si no queda nadie, la sala se cierra.")
    public ResponseEntity<StudyRoomDTO> leave(@PathVariable Long groupId, Authentication authentication) {
        return ResponseEntity.ok(studyRoomService.leave(groupId, authentication.getName()));
    }

    @PostMapping("/stop")
    @Operation(summary = "Terminar la sala", description = "Quien la inició o un moderador.")
    public ResponseEntity<StudyRoomDTO> stop(@PathVariable Long groupId, Authentication authentication) {
        return ResponseEntity.ok(studyRoomService.stop(groupId, authentication.getName()));
    }
}
