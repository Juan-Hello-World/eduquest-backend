package com.eduquest.api.service.impl;

import com.eduquest.api.dto.request.RecordingRequestDTO;
import com.eduquest.api.dto.request.SessionReviewRequestDTO;
import com.eduquest.api.dto.request.StudySessionRequestDTO;
import com.eduquest.api.dto.response.SessionReviewResponseDTO;
import com.eduquest.api.dto.response.StudySessionResponseDTO;
import com.eduquest.api.dto.response.UserResponseDTO;
import com.eduquest.api.entity.NotificationType;
import com.eduquest.api.entity.PrivateGroup;
import com.eduquest.api.entity.SessionAttendance;
import com.eduquest.api.entity.SessionReview;
import com.eduquest.api.entity.StudySession;
import com.eduquest.api.entity.User;
import com.eduquest.api.event.OnGroupActivityEvent;
import com.eduquest.api.event.OnUserActivityEvent;
import com.eduquest.api.exception.BadRequestException;
import com.eduquest.api.exception.DuplicateResourceException;
import com.eduquest.api.exception.ForbiddenException;
import com.eduquest.api.exception.InvalidOperationException;
import com.eduquest.api.exception.ResourceNotFoundException;
import com.eduquest.api.repository.PrivateGroupRepository;
import com.eduquest.api.repository.SessionReviewRepository;
import com.eduquest.api.repository.StudySessionRepository;
import com.eduquest.api.repository.UserRepository;
import com.eduquest.api.service.StudySessionService;
import com.eduquest.api.service.VideoMetadataService;
import com.eduquest.api.service.YouTubeLinks;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class StudySessionServiceImpl implements StudySessionService {

    private static final int MAX_DURATION_HOURS = 8;
    private static final int MAX_CALENDAR_DAYS = 62;

    private final StudySessionRepository sessionRepository;
    private final PrivateGroupRepository groupRepository;
    private final UserRepository userRepository;
    private final SessionReviewRepository reviewRepository;
    private final VideoMetadataService videoMetadataService;
    private final ApplicationEventPublisher eventPublisher;

    public StudySessionServiceImpl(StudySessionRepository sessionRepository,
                                   PrivateGroupRepository groupRepository,
                                   UserRepository userRepository,
                                   SessionReviewRepository reviewRepository,
                                   VideoMetadataService videoMetadataService,
                                   ApplicationEventPublisher eventPublisher) {
        this.sessionRepository = sessionRepository;
        this.groupRepository = groupRepository;
        this.userRepository = userRepository;
        this.reviewRepository = reviewRepository;
        this.videoMetadataService = videoMetadataService;
        this.eventPublisher = eventPublisher;
    }

    @Override
    @Transactional
    public StudySessionResponseDTO scheduleSession(Long groupId, StudySessionRequestDTO request, String username) {

        if (request.getScheduledAt().isBefore(LocalDateTime.now())){
           throw new InvalidOperationException("No se puede programar una sesión de estudio en una fecha pasada. ");
        }
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));


        PrivateGroup group = groupRepository.findById(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("Grupo privado no encontrado"));

        if (!groupRepository.existsByIdAndMembersId(groupId, user.getId())){
            throw new ForbiddenException("Debes ser miembro del grupo para programar sesiones de estudio");
        }

        StudySession session = new StudySession();
        session.setTitle(request.getTitle());
        session.setDescription(request.getDescription());
        session.setScheduledAt(request.getScheduledAt());
        session.setEndsAt(resolveEnd(request));
        session.setMeetingUrl(request.getMeetingUrl().trim());
        session.setPrivateGroup(group);
        session.setCreatedBy(user);
        LocalDateTime now = LocalDateTime.now();
        session.setCreatedAt(now);
        group.setLastActivityAt(now);

        StudySession saved = sessionRepository.save(session);
        eventPublisher.publishEvent(new OnGroupActivityEvent(
                this, groupId, user.getId(), NotificationType.GROUP_SESSION, saved.getTitle()));
        eventPublisher.publishEvent(new OnUserActivityEvent(this, user.getId()));
        return mapToResponse(saved, user);
    }


    @Override
    @Transactional(readOnly = true)
    public List<StudySessionResponseDTO> getMySessions(String username, LocalDate from, LocalDate to) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
        if (!to.isAfter(from)) {
            throw new BadRequestException("La fecha final debe ser posterior a la inicial");
        }
        if (from.plusDays(MAX_CALENDAR_DAYS).isBefore(to)) {
            throw new BadRequestException("El rango no puede superar los " + MAX_CALENDAR_DAYS + " días");
        }
        return sessionRepository.findForMemberBetween(user.getId(), from.atStartOfDay(), to.atStartOfDay()).stream()
                .map(session -> mapToResponse(session, user))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<StudySessionResponseDTO> getSessionsByGroup(Long groupId, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        if (!groupRepository.existsById(groupId)) {
            throw new ResourceNotFoundException("Grupo privado no encontrado");
        }

        if (!groupRepository.existsByIdAndMembersId(groupId, user.getId())){
            throw new ForbiddenException("Debes ser miembro del grupo para ver sus sesiones de estudio");
        }

        return sessionRepository.findByPrivateGroupId(groupId).stream()
                .map(session -> mapToResponse(session, user))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public StudySessionResponseDTO updateSession(Long groupId, Long sessionId,
                                                 StudySessionRequestDTO request, String username) {
        if (request.getScheduledAt().isBefore(LocalDateTime.now())) {
            throw new InvalidOperationException("No se puede mover una sesión de estudio a una fecha pasada.");
        }
        User user = findUser(username);
        StudySession session = findManageableSession(groupId, sessionId, user);

        session.setTitle(request.getTitle());
        session.setDescription(request.getDescription());
        session.setScheduledAt(request.getScheduledAt());
        session.setEndsAt(resolveEnd(request));
        session.setMeetingUrl(request.getMeetingUrl().trim());

        return mapToResponse(sessionRepository.save(session), user);
    }

    @Override
    @Transactional
    public void deleteSession(Long groupId, Long sessionId, String username) {
        User user = findUser(username);
        sessionRepository.delete(findManageableSession(groupId, sessionId, user));
    }

    @Override
    @Transactional
    public StudySessionResponseDTO setAttendance(Long groupId, Long sessionId, String username, boolean attending) {
        User user = findUser(username);
        requireMember(groupId, user);
        StudySession session = findSession(groupId, sessionId);
        if (session.hasStarted()) {
            throw new InvalidOperationException("La sesión ya empezó: entra con «Unirme ahora» para registrar tu asistencia");
        }

        boolean alreadyAttending = isAttending(session, user);
        if (attending && !alreadyAttending) {
            session.getAttendances().add(new SessionAttendance(session, user, LocalDateTime.now()));
            eventPublisher.publishEvent(new OnUserActivityEvent(this, user.getId()));
        } else if (!attending && alreadyAttending) {
            session.getAttendances().removeIf(attendance -> attendance.getUser().getId().equals(user.getId()));
        }
        return mapToResponse(sessionRepository.save(session), user);
    }

    @Override
    @Transactional
    public StudySessionResponseDTO checkIn(Long groupId, Long sessionId, String username) {
        User user = findUser(username);
        requireMember(groupId, user);
        StudySession session = findSession(groupId, sessionId);
        if (!session.isCheckInOpen()) {
            throw new InvalidOperationException(session.isFinished()
                    ? "La sesión ya terminó"
                    : "Podrás unirte desde " + StudySession.CHECK_IN_EARLY_MINUTES + " minutos antes de que empiece");
        }

        SessionAttendance attendance = session.getAttendances().stream()
                .filter(item -> item.getUser().getId().equals(user.getId()))
                .findFirst()
                .orElseGet(() -> {
                    SessionAttendance walkIn = new SessionAttendance(session, user, LocalDateTime.now());
                    walkIn.setConfirmed(false);
                    session.getAttendances().add(walkIn);
                    return walkIn;
                });
        if (attendance.getCheckedInAt() == null) {
            attendance.setCheckedInAt(LocalDateTime.now());
            eventPublisher.publishEvent(new OnUserActivityEvent(this, user.getId()));
        }
        return mapToResponse(sessionRepository.save(session), user);
    }

    private static LocalDateTime resolveEnd(StudySessionRequestDTO request) {
        LocalDateTime start = request.getScheduledAt();
        LocalDateTime end = request.getEndsAt() != null
                ? request.getEndsAt()
                : start.plusMinutes(StudySession.DEFAULT_DURATION_MINUTES);
        if (!end.isAfter(start)) {
            throw new InvalidOperationException("La hora de fin debe ser posterior a la de inicio");
        }
        if (end.isAfter(start.plusHours(MAX_DURATION_HOURS))) {
            throw new InvalidOperationException("Una sesión puede durar como máximo " + MAX_DURATION_HOURS + " horas");
        }
        return end;
    }

    @Override
    @Transactional
    public SessionReviewResponseDTO reviewSession(Long groupId, Long sessionId,
                                                  SessionReviewRequestDTO request, String username) {
        User user = findUser(username);
        requireMember(groupId, user);
        StudySession session = findSession(groupId, sessionId);
        if (!session.isFinished()) {
            throw new InvalidOperationException("Podrás calificar la asesoría cuando haya terminado");
        }
        if (isHost(session, user)) {
            throw new InvalidOperationException("No puedes calificar una asesoría que tú agendaste");
        }
        if (hasReviewed(session, user)) {
            throw new DuplicateResourceException("Ya calificaste esta asesoría");
        }

        SessionReview review = new SessionReview();
        review.setRating(request.getRating());
        review.setComment(request.getComment() == null || request.getComment().isBlank() ? null : request.getComment().trim());
        review.setSession(session);
        review.setAuthor(user);
        review.setCreatedAt(LocalDateTime.now());
        SessionReview saved = reviewRepository.save(review);
        session.getReviews().add(saved);
        eventPublisher.publishEvent(new OnUserActivityEvent(this, user.getId()));
        return mapReview(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SessionReviewResponseDTO> getSessionReviews(Long groupId, Long sessionId, String username) {
        User user = findUser(username);
        requireMember(groupId, user);
        findSession(groupId, sessionId);
        return reviewRepository.findBySessionIdOrderByCreatedAtDesc(sessionId).stream()
                .map(this::mapReview)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public StudySessionResponseDTO updateRecording(Long groupId, Long sessionId,
                                                   RecordingRequestDTO request, String username) {
        User user = findUser(username);
        StudySession session = findManageableSession(groupId, sessionId, user);

        String url = request.getRecordingUrl();
        if (url == null || url.isBlank()) {
            session.setRecordingUrl(null);
            session.setRecordingTitle(null);
        } else {
            VideoMetadataService.VideoInfo video = videoMetadataService.resolve(url.trim());
            session.setRecordingUrl(url.trim());
            session.setRecordingTitle(video.title());
        }
        return mapToResponse(sessionRepository.save(session), user);
    }

    private void requireMember(Long groupId, User user) {
        if (!groupRepository.existsByIdAndMembersId(groupId, user.getId())) {
            throw new ForbiddenException("Debes ser miembro del grupo");
        }
    }

    private StudySession findSession(Long groupId, Long sessionId) {
        return sessionRepository.findByIdAndPrivateGroupId(sessionId, groupId)
                .orElseThrow(() -> new ResourceNotFoundException("Sesión de estudio no encontrada"));
    }

    private boolean isHost(StudySession session, User user) {
        return session.getCreatedBy() != null && session.getCreatedBy().getId().equals(user.getId());
    }

    private boolean isAttending(StudySession session, User user) {
        return session.getAttendances().stream()
                .anyMatch(attendance -> attendance.isConfirmed() && attendance.getUser().getId().equals(user.getId()));
    }

    private boolean hasReviewed(StudySession session, User user) {
        return session.getReviews().stream()
                .anyMatch(review -> review.getAuthor().getId().equals(user.getId()));
    }

    private User findUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
    }

    private StudySession findManageableSession(Long groupId, Long sessionId, User user) {
        StudySession session = findSession(groupId, sessionId);
        if (!canManage(session, user)) {
            throw new ForbiddenException("Solo quien agendó la sesión o el creador del grupo pueden modificarla");
        }
        return session;
    }

    private boolean canManage(StudySession session, User user) {
        if (isHost(session, user)) {
            return true;
        }
        PrivateGroup group = session.getPrivateGroup();
        return group != null && group.getCreator() != null && group.getCreator().getId().equals(user.getId());
    }

    private StudySessionResponseDTO mapToResponse(StudySession session, User viewer) {
        StudySessionResponseDTO dto = new StudySessionResponseDTO();
        dto.setId(session.getId());
        dto.setTitle(session.getTitle());
        dto.setDescription(session.getDescription());
        dto.setScheduledAt(session.getScheduledAt());
        dto.setEndsAt(session.getEffectiveEnd());
        dto.setMeetingUrl(session.getMeetingUrl());
        dto.setGroupId(session.getPrivateGroup() != null ? session.getPrivateGroup().getId() : null);
        dto.setGroupName(session.getPrivateGroup() != null ? session.getPrivateGroup().getName() : null);
        dto.setCreatedByUsername(session.getCreatedBy() != null ? session.getCreatedBy().getUsername() : null);
        dto.setCanManage(canManage(session, viewer));
        dto.setCreatedByAdvisor(session.getCreatedBy() != null && session.getCreatedBy().isAdvisor());

        boolean finished = session.isFinished();
        boolean reviewedByMe = hasReviewed(session, viewer);
        dto.setFinished(finished);
        dto.setStarted(session.hasStarted());
        dto.setCheckInOpen(session.isCheckInOpen());
        dto.setAttendeeCount((int) session.getAttendances().stream().filter(SessionAttendance::isConfirmed).count());
        dto.setCheckedInCount((int) session.getAttendances().stream().filter(SessionAttendance::isCheckedIn).count());
        dto.setAttending(isAttending(session, viewer));
        dto.setCheckedIn(session.getAttendances().stream()
                .anyMatch(attendance -> attendance.isCheckedIn() && attendance.getUser().getId().equals(viewer.getId())));
        dto.setReviewCount(session.getReviews().size());
        dto.setAverageRating(session.getReviews().isEmpty() ? null : session.getReviews().stream()
                .mapToInt(SessionReview::getRating)
                .average()
                .orElse(0));
        dto.setReviewedByMe(reviewedByMe);
        dto.setCanReview(finished && !isHost(session, viewer) && !reviewedByMe);

        dto.setRecordingUrl(session.getRecordingUrl());
        dto.setRecordingTitle(session.getRecordingTitle());
        dto.setRecordingVideoId(YouTubeLinks.extractVideoId(session.getRecordingUrl()).orElse(null));
        return dto;
    }

    private SessionReviewResponseDTO mapReview(SessionReview review) {
        SessionReviewResponseDTO dto = new SessionReviewResponseDTO();
        dto.setId(review.getId());
        dto.setRating(review.getRating());
        dto.setComment(review.getComment());
        dto.setCreatedAt(review.getCreatedAt());
        User author = review.getAuthor();
        dto.setAuthorUsername(author.getUsername());
        dto.setAuthorAvatarUrl(UserResponseDTO.avatarUrlFor(author.getId(), author.getAvatarVersion()));
        return dto;
    }
}