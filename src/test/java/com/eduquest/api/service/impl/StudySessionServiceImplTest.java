package com.eduquest.api.service.impl;

import com.eduquest.api.dto.request.StudySessionRequestDTO;
import com.eduquest.api.dto.response.StudySessionResponseDTO;
import com.eduquest.api.entity.PrivateGroup;
import com.eduquest.api.entity.StudySession;
import com.eduquest.api.exception.ResourceNotFoundException;
import com.eduquest.api.repository.PrivateGroupRepository;
import com.eduquest.api.repository.StudySessionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class StudySessionServiceImplTest {

    @Mock StudySessionRepository sessionRepository;
    @Mock PrivateGroupRepository groupRepository;

    @InjectMocks StudySessionServiceImpl studySessionService;

    private PrivateGroup group;
    private LocalDateTime scheduled;

    @BeforeEach
    void setUp() {
        group = new PrivateGroup();
        group.setId(1L);
        group.setName("Grupo de Calculo");
        scheduled = LocalDateTime.now().plusDays(1);
    }

    @Test
    void createSession_withExistingGroup_persistsSession() {
        when(groupRepository.findById(1L)).thenReturn(Optional.of(group));
        when(sessionRepository.save(any(StudySession.class))).thenAnswer(invocation -> {
            StudySession saved = invocation.getArgument(0);
            saved.setId(1L);
            return saved;
        });

        StudySessionRequestDTO request = new StudySessionRequestDTO();
        request.setTitle("Sesion 1");
        request.setDescription("Repaso");
        request.setScheduledAt(scheduled);
        request.setMeetingUrl("https://meet.example/abc");

        StudySessionResponseDTO response = studySessionService.createSession(1L, request);

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getTitle()).isEqualTo("Sesion 1");
        assertThat(response.getScheduledAt()).isEqualTo(scheduled);
        assertThat(response.getGroupName()).isEqualTo("Grupo de Calculo");
    }

    @Test
    void createSession_withUnknownGroup_throwsNotFound() {
        when(groupRepository.findById(anyLong())).thenReturn(Optional.empty());

        StudySessionRequestDTO request = new StudySessionRequestDTO();
        request.setTitle("S");
        request.setScheduledAt(scheduled);

        assertThatThrownBy(() -> studySessionService.createSession(99L, request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getSessionsByGroup_returnsOnlySessionsOfThatGroup() {
        when(groupRepository.existsById(1L)).thenReturn(true);

        PrivateGroup other = new PrivateGroup();
        other.setId(2L);
        other.setName("Otro grupo");

        StudySession session1 = new StudySession();
        session1.setId(1L);
        session1.setTitle("De este grupo");
        session1.setScheduledAt(scheduled);
        session1.setPrivateGroup(group);

        StudySession session2 = new StudySession();
        session2.setId(2L);
        session2.setTitle("De otro grupo");
        session2.setScheduledAt(scheduled);
        session2.setPrivateGroup(other);

        when(sessionRepository.findAll()).thenReturn(List.of(session1, session2));

        List<StudySessionResponseDTO> sessions = studySessionService.getSessionsByGroup(1L);

        assertThat(sessions).hasSize(1);
        assertThat(sessions.get(0).getTitle()).isEqualTo("De este grupo");
    }

    @Test
    void getSessionsByGroup_withUnknownGroup_throwsNotFound() {
        when(groupRepository.existsById(anyLong())).thenReturn(false);

        assertThatThrownBy(() -> studySessionService.getSessionsByGroup(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}