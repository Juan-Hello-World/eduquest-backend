package com.eduquest.api.service;

import com.eduquest.api.dto.request.StudySessionRequestDTO;
import com.eduquest.api.dto.response.StudySessionResponseDTO;
import java.util.List;

public interface StudySessionService {
    StudySessionResponseDTO createSession(Long groupId, StudySessionRequestDTO request);
    List<StudySessionResponseDTO> getSessionsByGroup(Long groupId);
}