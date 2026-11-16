package com.eduquest.api.service.impl;

import com.eduquest.api.dto.response.BadgeDTO;
import com.eduquest.api.dto.response.GamificationProfileDTO;
import com.eduquest.api.dto.response.GamificationStatsDTO;
import com.eduquest.api.dto.response.LeaderboardEntryDTO;
import com.eduquest.api.dto.response.UserResponseDTO;
import com.eduquest.api.entity.User;
import com.eduquest.api.exception.ResourceNotFoundException;
import com.eduquest.api.repository.DocumentRepository;
import com.eduquest.api.repository.ReviewRepository;
import com.eduquest.api.repository.SessionAttendanceRepository;
import com.eduquest.api.repository.SessionReviewRepository;
import com.eduquest.api.repository.StudyPlanRepository;
import com.eduquest.api.repository.StudySessionRepository;
import com.eduquest.api.repository.UserRepository;
import com.eduquest.api.service.GamificationService;
import com.eduquest.api.service.NotificationService;
import com.eduquest.api.service.PlanSteps;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class GamificationServiceImpl implements GamificationService {

    static final int DOCUMENT_POINTS = 10;
    static final int REVIEW_POINTS = 5;
    static final int SESSION_POINTS = 8;
    static final int CONFIRMATION_POINTS = 1;
    static final int ATTENDANCE_POINTS = 3;
    static final int NO_SHOW_PENALTY = 3;
    static final int SESSION_REVIEW_POINTS = 4;
    private static final int LEADERBOARD_SIZE = 10;
    private static final LocalDateTime BEGINNING = LocalDateTime.of(2000, 1, 1, 0, 0);

    private final UserRepository userRepository;
    private final DocumentRepository documentRepository;
    private final ReviewRepository reviewRepository;
    private final StudySessionRepository sessionRepository;
    private final SessionAttendanceRepository attendanceRepository;
    private final SessionReviewRepository sessionReviewRepository;
    private final StudyPlanRepository studyPlanRepository;
    private final NotificationService notificationService;

    public GamificationServiceImpl(UserRepository userRepository,
                                   DocumentRepository documentRepository,
                                   ReviewRepository reviewRepository,
                                   StudySessionRepository sessionRepository,
                                   SessionAttendanceRepository attendanceRepository,
                                   SessionReviewRepository sessionReviewRepository,
                                   StudyPlanRepository studyPlanRepository,
                                   NotificationService notificationService) {
        this.userRepository = userRepository;
        this.documentRepository = documentRepository;
        this.reviewRepository = reviewRepository;
        this.sessionRepository = sessionRepository;
        this.attendanceRepository = attendanceRepository;
        this.sessionReviewRepository = sessionReviewRepository;
        this.studyPlanRepository = studyPlanRepository;
        this.notificationService = notificationService;
    }

    @Override
    @Transactional(readOnly = true)
    public GamificationProfileDTO getProfile(String username) {
        return profileOf(userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado")));
    }

    @Override
    @Transactional(readOnly = true)
    public GamificationProfileDTO getProfile(Long userId) {
        return profileOf(userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado")));
    }

    private GamificationProfileDTO profileOf(User user) {
        Long userId = user.getId();
        GamificationStatsDTO stats = statsFor(userId);

        long points = Math.max(0, pointsFor(stats));
        long weeklyPoints = Math.max(0, pointsSince(LocalDateTime.now().minusDays(7)).getOrDefault(userId, 0L));

        Integer rank = null;
        if (points > 0) {
            Map<Long, Long> allTime = pointsSince(BEGINNING);
            long ahead = allTime.entrySet().stream()
                    .filter(entry -> !entry.getKey().equals(userId) && entry.getValue() > points)
                    .count();
            rank = (int) ahead + 1;
        }

        return new GamificationProfileDTO(points, weeklyPoints, rank, stats, badgesFor(user, stats));
    }

    @Override
    @Transactional(readOnly = true)
    public List<LeaderboardEntryDTO> getLeaderboard(String period) {
        LocalDateTime since = "all".equalsIgnoreCase(period) ? BEGINNING : LocalDateTime.now().minusDays(7);
        Map<Long, Long> points = pointsSince(since);
        if (points.isEmpty()) {
            return List.of();
        }

        Map<Long, User> users = userRepository.findAllById(points.keySet()).stream()
                .filter(user -> !user.isBanned())
                .collect(Collectors.toMap(User::getId, Function.identity()));

        List<Map.Entry<Long, Long>> ranked = points.entrySet().stream()
                .filter(entry -> users.containsKey(entry.getKey()) && entry.getValue() > 0)
                .sorted(Map.Entry.<Long, Long>comparingByValue().reversed()
                        .thenComparing(entry -> users.get(entry.getKey()).getUsername().toLowerCase()))
                .limit(LEADERBOARD_SIZE)
                .toList();

        List<LeaderboardEntryDTO> leaderboard = new ArrayList<>();
        for (int i = 0; i < ranked.size(); i++) {
            User user = users.get(ranked.get(i).getKey());
            leaderboard.add(new LeaderboardEntryDTO(i + 1, user.getId(), user.getUsername(),
                    UserResponseDTO.avatarUrlFor(user.getId(), user.getAvatarVersion()),
                    user.isAdvisor(), ranked.get(i).getValue()));
        }
        return leaderboard;
    }

    @Override
    @Transactional
    public void syncBadges(Long userId) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return;
        }
        boolean changed = false;
        for (BadgeDTO badge : badgesFor(user, statsFor(userId))) {
            if (badge.earned() && user.getEarnedBadges().add(badge.code())) {
                notificationService.notifyBadge(user, badge.name(), badge.description());
                changed = true;
            }
        }
        if (changed) {
            userRepository.save(user);
        }
    }

    private GamificationStatsDTO statsFor(Long userId) {
        return new GamificationStatsDTO(
                documentRepository.countByAuthorId(userId),
                reviewRepository.countByAuthorId(userId),
                sessionRepository.countByCreatedById(userId),
                attendanceRepository.countConfirmedByUserId(userId),
                attendanceRepository.countByUserIdAndCheckedInAtIsNotNull(userId),
                attendanceRepository.countNoShowsByUserId(userId, LocalDateTime.now()),
                sessionReviewRepository.countByAuthorId(userId));
    }

    private Map<Long, Long> pointsSince(LocalDateTime since) {
        Map<Long, Long> points = new HashMap<>();
        addPoints(points, documentRepository.countByAuthorSince(since), DOCUMENT_POINTS);
        addPoints(points, reviewRepository.countByAuthorSince(since), REVIEW_POINTS);
        addPoints(points, sessionRepository.countByCreatorSince(since), SESSION_POINTS);
        addPoints(points, attendanceRepository.countConfirmedByUserSince(since), CONFIRMATION_POINTS);
        addPoints(points, attendanceRepository.countCheckInsByUserSince(since), ATTENDANCE_POINTS);
        addPoints(points, attendanceRepository.countNoShowsByUserSince(since, LocalDateTime.now()), -NO_SHOW_PENALTY);
        addPoints(points, sessionReviewRepository.countByAuthorSince(since), SESSION_REVIEW_POINTS);
        return points;
    }

    private static void addPoints(Map<Long, Long> points, List<Object[]> rows, int weight) {
        for (Object[] row : rows) {
            if (row[0] instanceof Long userId && row[1] instanceof Number count) {
                points.merge(userId, count.longValue() * weight, Long::sum);
            }
        }
    }

    private static long pointsFor(GamificationStatsDTO stats) {
        return stats.documents() * DOCUMENT_POINTS
                + stats.reviews() * REVIEW_POINTS
                + stats.sessionsHosted() * SESSION_POINTS
                + stats.confirmations() * CONFIRMATION_POINTS
                + stats.attendances() * ATTENDANCE_POINTS
                - stats.noShows() * NO_SHOW_PENALTY
                + stats.sessionReviews() * SESSION_REVIEW_POINTS;
    }

    private List<BadgeDTO> badgesFor(User user, GamificationStatsDTO stats) {
        List<BadgeDTO> badges = new ArrayList<>();
        badges.add(new BadgeDTO("FIRST_UPLOAD", "Primer aporte",
                "Comparte tu primer material", stats.documents() >= 1));
        badges.add(new BadgeDTO("LIBRARIAN", "Bibliotecario",
                "Comparte 10 materiales", stats.documents() >= 10));
        badges.add(new BadgeDTO("CRITIC", "Crítico",
                "Deja 5 reseñas de materiales", stats.reviews() >= 5));
        badges.add(new BadgeDTO("ORGANIZER", "Organizador",
                "Agenda 3 asesorías", stats.sessionsHosted() >= 3));
        badges.add(new BadgeDTO("LOYAL", "Asistente fiel",
                "Asiste a 5 asesorías", stats.attendances() >= 5));
        badges.add(new BadgeDTO("PLANNER", "Planificador",
                "Completa todos los pasos de un plan de estudio", hasFinishedPlan(user)));
        badges.add(new BadgeDTO("TOP_ADVISOR", "Asesor top",
                "Siendo asesor, logra un promedio de 4.5 o más en al menos 3 calificaciones", isTopAdvisor(user)));
        return badges;
    }

    private boolean hasFinishedPlan(User user) {
        return studyPlanRepository.findByUserId(user.getId()).stream().anyMatch(plan -> {
            int total = PlanSteps.count(plan.getGeneratedContent());
            return total > 0 && plan.getCompletedSteps() != null && plan.getCompletedSteps().size() >= total;
        });
    }

    private boolean isTopAdvisor(User user) {
        if (!user.isAdvisor() || sessionReviewRepository.countBySessionCreatedById(user.getId()) < 3) {
            return false;
        }
        Double average = sessionReviewRepository.averageRatingForHost(user.getId());
        return average != null && average >= 4.5;
    }
}
