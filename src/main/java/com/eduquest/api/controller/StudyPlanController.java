package com.eduquest.api.controller;

import com.eduquest.api.dto.request.StudyPlanProgressRequestDTO;
import com.eduquest.api.dto.request.StudyPlanRequestDTO;
import com.eduquest.api.dto.response.StudyPlanResponseDTO;
import com.eduquest.api.service.StudyPlanService;
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
@RequestMapping("/api/v1/study-plans")
@Tag(name = "Planes de estudio", description = "Generación de planes de estudio personalizados por curso y dificultad.")
public class StudyPlanController {

    private final StudyPlanService studyPlanService;

    public StudyPlanController(StudyPlanService studyPlanService) {
        this.studyPlanService = studyPlanService;
    }

    @PostMapping("/generate")
    @PreAuthorize("hasAuthority('ROLE_USER') or hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_MANAGER')")
    @Operation(summary = "Generar plan de estudio", description = "Devuelve un plan estructurado para un curso (courseName y difficulty) o para todo el ciclo (courses: lista de cursos con su dificultad, hasta 8).")
    public ResponseEntity<StudyPlanResponseDTO> generatePlan(
            @Valid @RequestBody StudyPlanRequestDTO request,
            Authentication authentication) {

        String username = authentication.getName();

        StudyPlanResponseDTO response = studyPlanService.generateStudyPlan(request, username);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping
    @PreAuthorize("hasAuthority('ROLE_USER') or hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_MANAGER')")
    @Operation(summary = "Listar mis planes de estudio", description = "Devuelve el historial de planes generados por el usuario autenticado.")
    public ResponseEntity<List<StudyPlanResponseDTO>> getMyPlans(Authentication authentication) {
        String username = authentication.getName();
        return ResponseEntity.ok(studyPlanService.getMyStudyPlans(username));
    }

    @PutMapping("/{planId}/progress")
    @PreAuthorize("hasAuthority('ROLE_USER') or hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_MANAGER')")
    @Operation(summary = "Guardar progreso del plan", description = "Recibe los índices (desde 0) de los pasos marcados como hechos. 404 si el plan no es del usuario.")
    public ResponseEntity<StudyPlanResponseDTO> updateProgress(
            @PathVariable Long planId,
            @Valid @RequestBody StudyPlanProgressRequestDTO request,
            Authentication authentication) {
        return ResponseEntity.ok(studyPlanService.updateProgress(planId, request, authentication.getName()));
    }
}
