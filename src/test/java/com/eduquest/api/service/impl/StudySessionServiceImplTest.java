package com.eduquest.api.service.impl;

import com.eduquest.api.dto.request.StudySessionRequestDTO;
import com.eduquest.api.dto.response.StudySessionResponseDTO;
import com.eduquest.api.entity.PrivateGroup;
import com.eduquest.api.entity.StudySession;
import com.eduquest.api.entity.User;
import com.eduquest.api.exception.ForbiddenException;
import com.eduquest.api.exception.InvalidOperationException;
import com.eduquest.api.exception.ResourceNotFoundException;
import com.eduquest.api.dto.request.RecordingRequestDTO;
import com.eduquest.api.dto.request.SessionReviewRequestDTO;
import com.eduquest.api.dto.response.SessionReviewResponseDTO;
import com.eduquest.api.entity.SessionAttendance;
import com.eduquest.api.entity.SessionReview;
import com.eduquest.api.exception.BadRequestException;
import com.eduquest.api.exception.DuplicateResourceException;
import com.eduquest.api.repository.PrivateGroupRepository;
import com.eduquest.api.repository.SessionReviewRepository;
import com.eduquest.api.service.VideoMetadataService;
import com.eduquest.api.repository.StudySessionRepository;
import com.eduquest.api.repository.UserRepository;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class StudySessionServiceImplTest {

    @Mock StudySessionRepository sessionRepository;
    @Mock PrivateGroupRepository groupRepository;
    @Mock UserRepository userRepository;
    @Mock SessionReviewRepository reviewRepository;
    @Mock VideoMetadataService videoMetadataService;
    @Mock org.springframework.context.ApplicationEventPublisher eventPublisher;

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
        assertThat(dto.getCreatedByUsername()).isEqualTo("alice");
        assertThat(dto.isCanManage()).isTrue();
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

    @Test
    void updateSession_byAuthor_changesDateAndLink() {
        StudySession session = sessionBy(user);
        LocalDateTime newDate = scheduled.plusDays(1);
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        when(sessionRepository.findByIdAndPrivateGroupId(5L, 1L)).thenReturn(Optional.of(session));
        when(sessionRepository.save(session)).thenReturn(session);

        StudySessionResponseDTO dto = studySessionService.updateSession(1L, 5L,
                request("Repaso final", newDate, " https://meet.google.com/xyz "), "alice");

        assertThat(dto.getTitle()).isEqualTo("Repaso final");
        assertThat(dto.getScheduledAt()).isEqualTo(newDate);
        assertThat(dto.getMeetingUrl()).isEqualTo("https://meet.google.com/xyz");
    }

    @Test
    void updateSession_byGroupCreator_isAllowedForLegacySessions() {
        group.setCreator(user);
        StudySession session = sessionBy(null);
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        when(sessionRepository.findByIdAndPrivateGroupId(5L, 1L)).thenReturn(Optional.of(session));
        when(sessionRepository.save(session)).thenReturn(session);

        StudySessionResponseDTO dto = studySessionService.updateSession(1L, 5L,
                request("Repaso", scheduled, "https://meet.google.com/abc"), "alice");

        assertThat(dto.isCanManage()).isTrue();
        assertThat(dto.getCreatedByUsername()).isNull();
    }

    @Test
    void updateSession_byOtherMember_throwsForbidden() {
        User bob = new User();
        bob.setId(2L);
        bob.setUsername("bob");
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        when(sessionRepository.findByIdAndPrivateGroupId(5L, 1L)).thenReturn(Optional.of(sessionBy(bob)));

        assertThatThrownBy(() -> studySessionService.updateSession(1L, 5L,
                request("Repaso", scheduled, "https://meet.google.com/abc"), "alice"))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void updateSession_toPastDate_throwsInvalidOperation() {
        assertThatThrownBy(() -> studySessionService.updateSession(1L, 5L,
                request("Repaso", LocalDateTime.now().minusDays(1), "https://meet.google.com/abc"), "alice"))
                .isInstanceOf(InvalidOperationException.class);
    }

    @Test
    void deleteSession_byAuthor_deletesIt() {
        StudySession session = sessionBy(user);
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        when(sessionRepository.findByIdAndPrivateGroupId(5L, 1L)).thenReturn(Optional.of(session));

        studySessionService.deleteSession(1L, 5L, "alice");

        verify(sessionRepository).delete(session);
    }

    @Test
    void deleteSession_fromAnotherGroup_throwsNotFound() {
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        when(sessionRepository.findByIdAndPrivateGroupId(5L, 2L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> studySessionService.deleteSession(2L, 5L, "alice"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    private StudySession sessionBy(User author) {
        StudySession session = new StudySession();
        session.setId(5L);
        session.setTitle("Repaso");
        session.setScheduledAt(scheduled);
        session.setMeetingUrl("https://meet.google.com/old");
        session.setPrivateGroup(group);
        session.setCreatedBy(author);
        return session;
    }

    private StudySessionRequestDTO request(String title, LocalDateTime date, String url) {
        StudySessionRequestDTO req = new StudySessionRequestDTO();
        req.setTitle(title);
        req.setScheduledAt(date);
        req.setMeetingUrl(url);
        return req;
    }

    private User otherUser() {
        User other = new User();
        other.setId(2L);
        other.setUsername("bob");
        return other;
    }

    private StudySession sessionHostedBy(User host, LocalDateTime when) {
        StudySession session = new StudySession();
        session.setId(5L);
        session.setTitle("Asesoría");
        session.setScheduledAt(when);
        session.setMeetingUrl("https://meet.example/abc");
        session.setPrivateGroup(group);
        session.setCreatedBy(host);
        return session;
    }

    private void memberSetup(User member, StudySession session) {
        when(userRepository.findByUsername(member.getUsername())).thenReturn(Optional.of(member));
        when(groupRepository.existsByIdAndMembersId(1L, member.getId())).thenReturn(true);
        when(sessionRepository.findByIdAndPrivateGroupId(5L, 1L)).thenReturn(Optional.of(session));
        when(sessionRepository.save(any(StudySession.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void setAttendance_confirmsAndCancels() {
        StudySession session = sessionHostedBy(user, scheduled);
        User bob = otherUser();
        memberSetup(bob, session);

        StudySessionResponseDTO attending = studySessionService.setAttendance(1L, 5L, "bob", true);
        assertThat(attending.isAttending()).isTrue();
        assertThat(attending.getAttendeeCount()).isEqualTo(1);

        studySessionService.setAttendance(1L, 5L, "bob", true);
        assertThat(session.getAttendances()).hasSize(1);

        StudySessionResponseDTO cancelled = studySessionService.setAttendance(1L, 5L, "bob", false);
        assertThat(cancelled.isAttending()).isFalse();
        assertThat(cancelled.getAttendeeCount()).isZero();
    }

    @Test
    void setAttendance_onFinishedSession_throwsInvalidOperation() {
        StudySession session = sessionHostedBy(user, LocalDateTime.now().minusHours(2));
        memberSetup(otherUser(), session);

        assertThatThrownBy(() -> studySessionService.setAttendance(1L, 5L, "bob", true))
                .isInstanceOf(InvalidOperationException.class);
    }

    @Test
    void setAttendance_notMember_throwsForbidden() {
        when(userRepository.findByUsername("bob")).thenReturn(Optional.of(otherUser()));
        when(groupRepository.existsByIdAndMembersId(1L, 2L)).thenReturn(false);

        assertThatThrownBy(() -> studySessionService.setAttendance(1L, 5L, "bob", true))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void reviewSession_afterItEnds_savesReviewAndUpdatesSummary() {
        StudySession session = sessionHostedBy(user, LocalDateTime.now().minusDays(1));
        User bob = otherUser();
        memberSetup(bob, session);
        when(reviewRepository.save(any(SessionReview.class))).thenAnswer(inv -> {
            SessionReview review = inv.getArgument(0);
            review.setId(7L);
            return review;
        });

        SessionReviewRequestDTO request = new SessionReviewRequestDTO();
        request.setRating(4);
        request.setComment("  Muy clara  ");

        SessionReviewResponseDTO review = studySessionService.reviewSession(1L, 5L, request, "bob");

        assertThat(review.getId()).isEqualTo(7L);
        assertThat(review.getComment()).isEqualTo("Muy clara");
        assertThat(review.getAuthorUsername()).isEqualTo("bob");

        when(sessionRepository.findByPrivateGroupId(1L)).thenReturn(List.of(session));
        when(groupRepository.existsById(1L)).thenReturn(true);
        StudySessionResponseDTO dto = studySessionService.getSessionsByGroup(1L, "bob").get(0);
        assertThat(dto.isFinished()).isTrue();
        assertThat(dto.getReviewCount()).isEqualTo(1);
        assertThat(dto.getAverageRating()).isEqualTo(4.0);
        assertThat(dto.isReviewedByMe()).isTrue();
        assertThat(dto.isCanReview()).isFalse();
    }

    @Test
    void reviewSession_beforeItEnds_throwsInvalidOperation() {
        memberSetup(otherUser(), sessionHostedBy(user, scheduled));
        SessionReviewRequestDTO request = new SessionReviewRequestDTO();
        request.setRating(5);

        assertThatThrownBy(() -> studySessionService.reviewSession(1L, 5L, request, "bob"))
                .isInstanceOf(InvalidOperationException.class);
    }

    @Test
    void reviewSession_byHost_throwsInvalidOperation() {
        memberSetup(user, sessionHostedBy(user, LocalDateTime.now().minusDays(1)));
        SessionReviewRequestDTO request = new SessionReviewRequestDTO();
        request.setRating(5);

        assertThatThrownBy(() -> studySessionService.reviewSession(1L, 5L, request, "alice"))
                .isInstanceOf(InvalidOperationException.class);
    }

    @Test
    void reviewSession_twice_throwsDuplicate() {
        StudySession session = sessionHostedBy(user, LocalDateTime.now().minusDays(1));
        User bob = otherUser();
        SessionReview existing = new SessionReview();
        existing.setAuthor(bob);
        existing.setRating(3);
        session.getReviews().add(existing);
        memberSetup(bob, session);
        SessionReviewRequestDTO request = new SessionReviewRequestDTO();
        request.setRating(5);

        assertThatThrownBy(() -> studySessionService.reviewSession(1L, 5L, request, "bob"))
                .isInstanceOf(DuplicateResourceException.class);
    }

    @Test
    void getSessionReviews_returnsMappedReviews() {
        StudySession session = sessionHostedBy(user, LocalDateTime.now().minusDays(1));
        User bob = otherUser();
        memberSetup(bob, session);
        SessionReview review = new SessionReview();
        review.setId(1L);
        review.setRating(5);
        review.setAuthor(bob);
        when(reviewRepository.findBySessionIdOrderByCreatedAtDesc(5L)).thenReturn(List.of(review));

        assertThat(studySessionService.getSessionReviews(1L, 5L, "bob"))
                .extracting(SessionReviewResponseDTO::getRating).containsExactly(5);
    }

    @Test
    void getSessionReviews_missingSession_throwsNotFound() {
        memberSetup(otherUser(), sessionHostedBy(user, scheduled));
        when(sessionRepository.findByIdAndPrivateGroupId(9L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> studySessionService.getSessionReviews(1L, 9L, "bob"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void updateRecording_setsValidatedVideoAndCanClearIt() {
        StudySession session = sessionHostedBy(user, LocalDateTime.now().minusDays(1));
        memberSetup(user, session);
        when(videoMetadataService.resolve("https://youtu.be/dQw4w9WgXcQ"))
                .thenReturn(new VideoMetadataService.VideoInfo("dQw4w9WgXcQ", "Clase grabada"));

        RecordingRequestDTO request = new RecordingRequestDTO();
        request.setRecordingUrl(" https://youtu.be/dQw4w9WgXcQ ");
        StudySessionResponseDTO dto = studySessionService.updateRecording(1L, 5L, request, "alice");

        assertThat(dto.getRecordingTitle()).isEqualTo("Clase grabada");
        assertThat(dto.getRecordingVideoId()).isEqualTo("dQw4w9WgXcQ");

        request.setRecordingUrl("  ");
        StudySessionResponseDTO cleared = studySessionService.updateRecording(1L, 5L, request, "alice");
        assertThat(cleared.getRecordingUrl()).isNull();
        assertThat(cleared.getRecordingVideoId()).isNull();
    }

    @Test
    void updateRecording_byNonManager_throwsForbidden() {
        memberSetup(otherUser(), sessionHostedBy(user, scheduled));
        RecordingRequestDTO request = new RecordingRequestDTO();
        request.setRecordingUrl("https://youtu.be/dQw4w9WgXcQ");

        assertThatThrownBy(() -> studySessionService.updateRecording(1L, 5L, request, "bob"))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void updateRecording_invalidVideo_propagatesBadRequest() {
        memberSetup(user, sessionHostedBy(user, scheduled));
        when(videoMetadataService.resolve("https://example.com/video"))
                .thenThrow(new BadRequestException("Pega un enlace de YouTube válido"));
        RecordingRequestDTO request = new RecordingRequestDTO();
        request.setRecordingUrl("https://example.com/video");

        assertThatThrownBy(() -> studySessionService.updateRecording(1L, 5L, request, "alice"))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void mapToResponse_marksAdvisorHostAndAttendance() {
        user.setAdvisor(true);
        StudySession session = sessionHostedBy(user, scheduled);
        session.getAttendances().add(new SessionAttendance(session, user, LocalDateTime.now()));
        memberSetup(user, session);
        when(groupRepository.existsById(1L)).thenReturn(true);
        when(sessionRepository.findByPrivateGroupId(1L)).thenReturn(List.of(session));

        StudySessionResponseDTO dto = studySessionService.getSessionsByGroup(1L, "alice").get(0);

        assertThat(dto.isCreatedByAdvisor()).isTrue();
        assertThat(dto.isAttending()).isTrue();
        assertThat(dto.isCanReview()).isFalse();
        assertThat(dto.getAverageRating()).isNull();
    }
}
