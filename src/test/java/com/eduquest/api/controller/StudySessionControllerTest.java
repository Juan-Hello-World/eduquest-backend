package com.eduquest.api.controller;

import com.eduquest.api.dto.request.StudySessionRequestDTO;
import com.eduquest.api.dto.response.StudySessionResponseDTO;
import com.eduquest.api.service.StudySessionService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class StudySessionControllerTest {

    @Mock StudySessionService studySessionService;

    @InjectMocks StudySessionController studySessionController;

    private StudySessionResponseDTO response() {
        StudySessionResponseDTO dto = new StudySessionResponseDTO();
        dto.setId(1L);
        dto.setTitle("Sesion 1");
        dto.setScheduledAt(LocalDateTime.now().plusDays(1));
        dto.setMeetingUrl("https://meet.example/abc");
        dto.setGroupName("Grupo de Calculo");
        return dto;
    }

    @Test
    void createSession_returnsCreated() {
        when(studySessionService.createSession(anyLong(), any())).thenReturn(response());

        StudySessionRequestDTO request = new StudySessionRequestDTO();
        request.setTitle("Sesion 1");
        request.setScheduledAt(LocalDateTime.now().plusDays(1));

        ResponseEntity<StudySessionResponseDTO> result = studySessionController.createSession(1L, request);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(result.getBody().getTitle()).isEqualTo("Sesion 1");
    }

    @Test
    void getSessionsByGroup_returnsOk() {
        when(studySessionService.getSessionsByGroup(anyLong())).thenReturn(List.of(response()));

        ResponseEntity<List<StudySessionResponseDTO>> result = studySessionController.getSessionsByGroup(1L);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).hasSize(1);
    }
}