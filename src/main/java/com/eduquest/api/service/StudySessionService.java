package com.eduquest.api.service;

import com.eduquest.api.dto.request.StudySessionRequestDTO;
import com.eduquest.api.dto.response.StudySessionResponseDTO;
import java.util.List;

public interface StudySessionService {
    StudySessionResponseDTO scheduleSession(Long groupId, StudySessionRequestDTO request, String username);
    List<StudySessionResponseDTO> getSessionsByGroup(Long groupId, String username);
}