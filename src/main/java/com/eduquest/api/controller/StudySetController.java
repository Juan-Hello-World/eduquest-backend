package com.eduquest.api.controller;

import com.eduquest.api.dto.response.StudySetResponseDTO;
import com.eduquest.api.entity.StudySetType;
import com.eduquest.api.service.StudySetService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/documents/{documentId}/study-set")
@PreAuthorize("hasAuthority('ROLE_USER') or hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_MANAGER')")
@Tag(name = "Estudiar con IA", description = "Quiz y flashcards generados a partir de un material de la biblioteca.")
public class StudySetController {

    private final StudySetService studySetService;

    public StudySetController(StudySetService studySetService) {
        this.studySetService = studySetService;
    }

    @PostMapping
    @Operation(summary = "Obtener o generar quiz/flashcards",
            description = "type=QUIZ o FLASHCARDS. Se reutiliza el último generado salvo que refresh=true.")
    public ResponseEntity<StudySetResponseDTO> getStudySet(@PathVariable Long documentId,
                                                           @RequestParam StudySetType type,
                                                           @RequestParam(defaultValue = "false") boolean refresh,
                                                           Authentication authentication) {
        return ResponseEntity.ok(studySetService.getStudySet(documentId, type, refresh, authentication.getName()));
    }
}
