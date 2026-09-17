package com.eduquest.api.controller;

import com.eduquest.api.dto.request.StudySessionRequestDTO;
import com.eduquest.api.dto.response.StudySessionResponseDTO;
import com.eduquest.api.service.StudySessionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/groups/{groupId}/sessions")
@CrossOrigin(origins = "*")
public class StudySessionController {

    private final StudySessionService studySessionService;

    public StudySessionController(StudySessionService studySessionService) {
        this.studySessionService = studySessionService;
    }

    @PostMapping
    public ResponseEntity<StudySessionResponseDTO> createSession(
            @PathVariable Long groupId,
            @Valid @RequestBody StudySessionRequestDTO request) {
        StudySessionResponseDTO response = studySessionService.createSession(groupId, request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<StudySessionResponseDTO>> getSessionsByGroup(@PathVariable Long groupId) {
        List<StudySessionResponseDTO> responses = studySessionService.getSessionsByGroup(groupId);
        return new ResponseEntity<>(responses, HttpStatus.OK);
    }
}