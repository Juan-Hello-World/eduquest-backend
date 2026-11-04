package com.eduquest.api.service.impl;

import com.eduquest.api.dto.request.StudySessionRequestDTO;
import com.eduquest.api.dto.response.StudySessionResponseDTO;
import com.eduquest.api.entity.PrivateGroup;
import com.eduquest.api.entity.StudySession;
import com.eduquest.api.entity.User;
import com.eduquest.api.exception.ForbiddenException;
import com.eduquest.api.exception.InvalidOperationException;
import com.eduquest.api.exception.ResourceNotFoundException;
import com.eduquest.api.repository.PrivateGroupRepository;
import com.eduquest.api.repository.StudySessionRepository;
import com.eduquest.api.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StudySessionServiceImplTest {

    @Mock StudySessionRepository sessionRepository;
    @Mock PrivateGroupRepository groupRepository;
    @Mock UserRepository userRepository;

    @InjectMocks StudySessionServiceImpl studySessionService;

    private PrivateGroup group;
    private User user;
    private LocalDateTime scheduled;

    @BeforeEach
    void setUp() {
        group = new PrivateGroup();
        group.setId(1L);
        group.setName("Grupo Calculo");

        user = new User();
        user.setId(1L);
        user.setUsername("alice");

        scheduled = LocalDateTime.now().plusDays(2);
    }

    @Test
    void scheduleSession_validRequest_savesAndReturnsDto() {
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        when(groupRepository.findById(1L)).thenReturn(Optional.of(group));
        when(groupRepository.existsByIdAndMembersId(1L, 1L)).thenReturn(true);
        when(sessionRepository.save(any(StudySession.class))).thenAnswer(inv -> {
            StudySession s = inv.getArgument(0);
            s.setId(10L);
            return s;
        });

        StudySessionRequestDTO req = new StudySessionRequestDTO();
        req.setTitle("Repaso parcial");
        req.setDescription("Integrales");
        req.setScheduledAt(scheduled);
        req.setMeetingUrl("https://meet.example/abc");

        StudySessionResponseDTO dto = studySessionService.scheduleSession(1L, req, "alice");

        assertThat(dto.getId()).isEqualTo(10L);
        assertThat(dto.getTitle()).isEqualTo("Repaso parcial");
        assertThat(dto.getGroupName()).isEqualTo("Grupo Calculo");
    }

    @Test
    void scheduleSession_whenPastDate_throwsInvalidOperation() {
        StudySessionRequestDTO req = new StudySessionRequestDTO();
        req.setTitle("Pasada");
        req.setScheduledAt(LocalDateTime.now().minusDays(1));

        assertThatThrownBy(() -> studySessionService.scheduleSession(1L, req, "alice"))
                .isInstanceOf(InvalidOperationException.class);
    }

    @Test
    void scheduleSession_whenGroupMissing_throwsNotFound() {
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        when(groupRepository.findById(99L)).thenReturn(Optional.empty());

        StudySessionRequestDTO req = new StudySessionRequestDTO();
        req.setTitle("T");
        req.setScheduledAt(scheduled);

        assertThatThrownBy(() -> studySessionService.scheduleSession(99L, req, "alice"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void scheduleSession_whenNotMember_throwsForbidden() {
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        when(groupRepository.findById(1L)).thenReturn(Optional.of(group));
        when(groupRepository.existsByIdAndMembersId(1L, 1L)).thenReturn(false);

        StudySessionRequestDTO req = new StudySessionRequestDTO();
        req.setTitle("T");
        req.setScheduledAt(scheduled);

        assertThatThrownBy(() -> studySessionService.scheduleSession(1L, req, "alice"))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void getSessionsByGroup_returnsOnlySessionsOfThatGroup() {
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        when(groupRepository.existsById(1L)).thenReturn(true);
        when(groupRepository.existsByIdAndMembersId(1L, 1L)).thenReturn(true);

        StudySession session1 = new StudySession();
        session1.setId(1L);
        session1.setTitle("De este grupo");
        session1.setScheduledAt(scheduled);
        session1.setPrivateGroup(group);

        when(sessionRepository.findByPrivateGroupId(1L)).thenReturn(List.of(session1));

        List<StudySessionResponseDTO> sessions = studySessionService.getSessionsByGroup(1L, "alice");

        assertThat(sessions).hasSize(1);
        assertThat(sessions.get(0).getTitle()).isEqualTo("De este grupo");
    }

    @Test
    void getSessionsByGroup_whenGroupMissing_throwsNotFound() {
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        when(groupRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> studySessionService.getSessionsByGroup(99L, "alice"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getSessionsByGroup_whenNotMember_throwsForbidden() {
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        when(groupRepository.existsById(1L)).thenReturn(true);
        when(groupRepository.existsByIdAndMembersId(1L, 1L)).thenReturn(false);

        assertThatThrownBy(() -> studySessionService.getSessionsByGroup(1L, "alice"))
                .isInstanceOf(ForbiddenException.class);
    }
}