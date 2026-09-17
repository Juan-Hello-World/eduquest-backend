package com.eduquest.api.controller;

import com.eduquest.api.dto.request.StudyPlanRequestDTO;
import com.eduquest.api.dto.response.StudyPlanResponseDTO;
import com.eduquest.api.service.StudyPlanService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/study-plans")
@CrossOrigin(origins = "*")
public class StudyPlanController {

    private final StudyPlanService studyPlanService;

    public StudyPlanController(StudyPlanService studyPlanService) {
        this.studyPlanService = studyPlanService;
    }

    @PostMapping("/generate")
    public ResponseEntity<StudyPlanResponseDTO> generatePlan(
            @Valid @RequestBody StudyPlanRequestDTO request,
            Authentication authentication) {

        // Obtenemos el username del token JWT automáticamente inyectado por Spring Security
        String username = authentication.getName();

        StudyPlanResponseDTO response = studyPlanService.generateStudyPlan(request, username);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }
}