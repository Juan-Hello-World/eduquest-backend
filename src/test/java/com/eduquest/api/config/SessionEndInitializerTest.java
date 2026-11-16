package com.eduquest.api.config;

import com.eduquest.api.entity.StudySession;
import com.eduquest.api.repository.StudySessionRepository;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SessionEndInitializerTest {

    @Test
    void run_fillsMissingEndWithDefaultDuration() {
        StudySessionRepository sessionRepository = mock(StudySessionRepository.class);
        LocalDateTime start = LocalDateTime.of(2026, 11, 20, 18, 0);
        StudySession session = new StudySession();
        session.setScheduledAt(start);
        when(sessionRepository.findByEndsAtIsNull()).thenReturn(List.of(session));

        new SessionEndInitializer(sessionRepository).run();

        assertThat(session.getEndsAt()).isEqualTo(start.plusMinutes(StudySession.DEFAULT_DURATION_MINUTES));
        verify(sessionRepository).saveAll(List.of(session));
    }
}
