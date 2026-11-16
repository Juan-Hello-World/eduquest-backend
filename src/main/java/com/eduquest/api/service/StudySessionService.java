package com.eduquest.api.service;

import com.eduquest.api.dto.request.RecordingRequestDTO;
import com.eduquest.api.dto.request.SessionReviewRequestDTO;
import com.eduquest.api.dto.request.StudySessionRequestDTO;
import com.eduquest.api.dto.response.SessionReviewResponseDTO;
import com.eduquest.api.dto.response.StudySessionResponseDTO;

import java.time.LocalDate;
import java.util.List;

public interface StudySessionService {
    StudySessionResponseDTO scheduleSession(Long groupId, StudySessionRequestDTO request, String username);
    List<StudySessionResponseDTO> getMySessions(String username, LocalDate from, LocalDate to);
    List<StudySessionResponseDTO> getSessionsByGroup(Long groupId, String username);
    StudySessionResponseDTO updateSession(Long groupId, Long sessionId, StudySessionRequestDTO request, String username);
    void deleteSession(Long groupId, Long sessionId, String username);
    StudySessionResponseDTO setAttendance(Long groupId, Long sessionId, String username, boolean attending);
    StudySessionResponseDTO checkIn(Long groupId, Long sessionId, String username);
    SessionReviewResponseDTO reviewSession(Long groupId, Long sessionId, SessionReviewRequestDTO request, String username);
    List<SessionReviewResponseDTO> getSessionReviews(Long groupId, Long sessionId, String username);
    StudySessionResponseDTO updateRecording(Long groupId, Long sessionId, RecordingRequestDTO request, String username);
}