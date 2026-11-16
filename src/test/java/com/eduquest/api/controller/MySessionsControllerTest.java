package com.eduquest.api.controller;

import com.eduquest.api.dto.response.StudySessionResponseDTO;
import com.eduquest.api.service.StudySessionService;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MySessionsControllerTest {

    private final StudySessionService sessionService = mock(StudySessionService.class);
    private final MySessionsController controller = new MySessionsController(sessionService);

    @Test
    void getMySessions_delegatesWithRange() {
        LocalDate from = LocalDate.of(2026, 11, 1);
        LocalDate to = LocalDate.of(2026, 12, 1);
        StudySessionResponseDTO session = new StudySessionResponseDTO();
        session.setId(1L);
        when(sessionService.getMySessions("alice", from, to)).thenReturn(List.of(session));

        var response = controller.getMySessions(from, to, new UsernamePasswordAuthenticationToken("alice", null));

        assertThat(response.getBody()).containsExactly(session);
    }
}
