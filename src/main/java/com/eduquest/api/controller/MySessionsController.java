package com.eduquest.api.controller;

import com.eduquest.api.dto.response.StudySessionResponseDTO;
import com.eduquest.api.service.StudySessionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/sessions")
@Tag(name = "Sesiones de estudio")
public class MySessionsController {

    private final StudySessionService studySessionService;

    public MySessionsController(StudySessionService studySessionService) {
        this.studySessionService = studySessionService;
    }

    @GetMapping("/mine")
    @Operation(summary = "Mi calendario", description = "Sesiones de todos mis grupos entre from (incluido) y to (excluido), máximo 62 días.")
    public ResponseEntity<List<StudySessionResponseDTO>> getMySessions(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            Authentication authentication) {
        return ResponseEntity.ok(studySessionService.getMySessions(authentication.getName(), from, to));
    }
}
