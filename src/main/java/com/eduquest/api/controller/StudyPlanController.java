package com.eduquest.api.controller;

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

@RestController
@RequestMapping("/api/v1/study-plans")
@Tag(name = "Planes de estudio (IA)", description = "Generación de planes de estudio con OpenAI. Requiere OPENAI_API_KEY configurada.")
public class StudyPlanController {

    private final StudyPlanService studyPlanService;

    public StudyPlanController(StudyPlanService studyPlanService) {
        this.studyPlanService = studyPlanService;
    }

    @PostMapping("/generate")
    @PreAuthorize("hasAuthority('ROLE_USER') or hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_MANAGER')")
    @Operation(summary = "Generar plan de estudio con IA", description = "Devuelve un plan estructurado generado con OpenAI para el curso, duración y dificultad indicados.")
    public ResponseEntity<StudyPlanResponseDTO> generatePlan(
            @Valid @RequestBody StudyPlanRequestDTO request,
            Authentication authentication) {

        // Obtenemos el username del token JWT automáticamente inyectado por Spring Security
        String username = authentication.getName();

        StudyPlanResponseDTO response = studyPlanService.generateStudyPlan(request, username);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }
}