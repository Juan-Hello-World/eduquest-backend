package com.eduquest.api.controller;

import com.eduquest.api.dto.request.StudySessionRequestDTO;
import com.eduquest.api.dto.response.StudySessionResponseDTO;
import com.eduquest.api.service.StudySessionService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StudySessionControllerTest {

    @Mock StudySessionService sessionService;
    @InjectMocks StudySessionController studySessionController;

    private StudySessionResponseDTO response() {
        StudySessionResponseDTO r = new StudySessionResponseDTO();
        r.setId(1L);
        r.setTitle("Repaso Parcial");
        r.setDescription("Integrales");
        r.setScheduledAt(LocalDateTime.now().plusDays(2));
        r.setMeetingUrl("https://meet.example/abc");
        r.setGroupName("Grupo Calculo");
        return r;
    }

    @Test
    void scheduleSession_returnsCreated() {
        Authentication authentication = mock(Authentication.class);
        when(authentication.getName()).thenReturn("alice");
        when(sessionService.scheduleSession(eq(1L), any(StudySessionRequestDTO.class), anyString()))
                .thenReturn(response());

        StudySessionRequestDTO req = new StudySessionRequestDTO();
        req.setTitle("Repaso Parcial");
        req.setDescription("Integrales");
        req.setScheduledAt(LocalDateTime.now().plusDays(2));
        req.setMeetingUrl("https://meet.example/abc");

        ResponseEntity<StudySessionResponseDTO> result =
                studySessionController.scheduleSession(1L, req, authentication);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(result.getBody().getTitle()).isEqualTo("Repaso Parcial");
    }

    @Test
    void getSessionsByGroup_returnsOkList() {
        Authentication authentication = mock(Authentication.class);
        when(authentication.getName()).thenReturn("alice");
        when(sessionService.getSessionsByGroup(eq(1L), anyString())).thenReturn(List.of(response()));

        ResponseEntity<List<StudySessionResponseDTO>> result =
                studySessionController.getSessionsByGroup(1L, authentication);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).hasSize(1);
    }
}