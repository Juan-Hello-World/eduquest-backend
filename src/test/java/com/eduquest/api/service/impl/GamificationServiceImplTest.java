package com.eduquest.api.service.impl;

import com.eduquest.api.dto.response.BadgeDTO;
import com.eduquest.api.dto.response.GamificationProfileDTO;
import com.eduquest.api.dto.response.LeaderboardEntryDTO;
import com.eduquest.api.entity.StudyPlan;
import com.eduquest.api.entity.User;
import com.eduquest.api.exception.ResourceNotFoundException;
import com.eduquest.api.repository.DocumentRepository;
import com.eduquest.api.repository.ForumAnswerRepository;
import com.eduquest.api.repository.ForumVoteRepository;
import com.eduquest.api.repository.ReviewRepository;
import com.eduquest.api.repository.SessionAttendanceRepository;
import com.eduquest.api.repository.SessionReviewRepository;
import com.eduquest.api.repository.StudyPlanRepository;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyIterable;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class GamificationServiceImplTest {

    @Mock UserRepository userRepository;
    @Mock DocumentRepository documentRepository;
    @Mock ReviewRepository reviewRepository;
    @Mock StudySessionRepository sessionRepository;
    @Mock SessionAttendanceRepository attendanceRepository;
    @Mock SessionReviewRepository sessionReviewRepository;
    @Mock StudyPlanRepository studyPlanRepository;
    @Mock ForumAnswerRepository forumAnswerRepository;
    @Mock ForumVoteRepository forumVoteRepository;
    @Mock com.eduquest.api.service.NotificationService notificationService;

    @InjectMocks GamificationServiceImpl gamificationService;

    private User alice;
    private User bob;
    private User banned;

    @BeforeEach
    void setUp() {
        alice = user(1L, "alice");
        bob = user(2L, "bob");
        banned = user(3L, "troll");
        banned.setBanned(true);
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(alice));
        when(documentRepository.countByAuthorSince(any())).thenReturn(rows(new Object[]{1L, 2L}, new Object[]{3L, 9L}));
        when(reviewRepository.countByAuthorSince(any())).thenReturn(rows(new Object[]{2L, 3L}));
        when(sessionRepository.countAttendedByCreatorSince(any())).thenReturn(rows(new Object[]{2L, 1L}, new Object[]{null, 4L}));
        when(attendanceRepository.countCheckInsByUserSince(any())).thenReturn(rows(new Object[]{1L, 1L}));
        when(sessionReviewRepository.countByAuthorSince(any())).thenReturn(List.of());
    }

    private static User user(Long id, String username) {
        User user = new User();
        user.setId(id);
        user.setUsername(username);
        return user;
    }

    private static List<Object[]> rows(Object[]... rows) {
        return new ArrayList<>(List.of(rows));
    }

    @Test
    void getProfile_computesPointsRankAndBadges() {
        alice.setAdvisor(true);
        when(documentRepository.countByAuthorId(1L)).thenReturn(2L);
        when(reviewRepository.countByAuthorId(1L)).thenReturn(5L);
        when(sessionRepository.countByCreatedById(1L)).thenReturn(3L);
        when(attendanceRepository.countByUserIdAndCheckedInAtIsNotNull(1L)).thenReturn(1L);
        when(sessionReviewRepository.countByAuthorId(1L)).thenReturn(0L);
        when(sessionReviewRepository.countBySessionCreatedById(1L)).thenReturn(3L);
        when(sessionReviewRepository.averageRatingForHost(1L)).thenReturn(4.7);
        StudyPlan plan = new StudyPlan();
        plan.setGeneratedContent("- Leer\n- Practicar");
        plan.setCompletedSteps(Set.of(0, 1));
        when(studyPlanRepository.findByUserId(1L)).thenReturn(List.of(plan));

        GamificationProfileDTO profile = gamificationService.getProfile("alice");

        assertThat(profile.points()).isEqualTo(2 * 5 + 5);
        assertThat(profile.weeklyPoints()).isEqualTo(15);
        assertThat(profile.rank()).isEqualTo(3);
        assertThat(profile.badges()).filteredOn(BadgeDTO::earned).extracting(BadgeDTO::code)
                .containsExactlyInAnyOrder("FIRST_UPLOAD", "CRITIC", "ORGANIZER", "PLANNER", "TOP_ADVISOR");
    }

    @Test
    void getProfile_withoutActivity_hasNoRankNorBadges() {
        when(studyPlanRepository.findByUserId(1L)).thenReturn(List.of());
        when(documentRepository.countByAuthorSince(any())).thenReturn(List.of());
        when(attendanceRepository.countCheckInsByUserSince(any())).thenReturn(List.of());

        GamificationProfileDTO profile = gamificationService.getProfile("alice");

        assertThat(profile.points()).isZero();
        assertThat(profile.rank()).isNull();
        assertThat(profile.badges()).noneMatch(BadgeDTO::earned);
    }

    @Test
    void getProfile_advisorWithLowRating_isNotTopAdvisor() {
        alice.setAdvisor(true);
        when(sessionReviewRepository.countBySessionCreatedById(1L)).thenReturn(5L);
        when(sessionReviewRepository.averageRatingForHost(1L)).thenReturn(3.9);

        assertThat(gamificationService.getProfile("alice").badges())
                .filteredOn(badge -> badge.code().equals("TOP_ADVISOR"))
                .allMatch(badge -> !badge.earned());
    }

    @Test
    void getProfile_unknownUser_throwsNotFound() {
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> gamificationService.getProfile("ghost")).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getLeaderboard_sortsByPointsAndSkipsBannedUsers() {
        when(reviewRepository.countByAuthorSince(any())).thenReturn(rows(new Object[]{2L, 4L}));
        when(userRepository.findAllById(anyIterable())).thenReturn(List.of(alice, bob, banned));

        List<LeaderboardEntryDTO> leaderboard = gamificationService.getLeaderboard("all");

        assertThat(leaderboard).extracting(LeaderboardEntryDTO::username).containsExactly("bob", "alice");
        assertThat(leaderboard.get(0).points()).isEqualTo(4 * 2 + 10);
        assertThat(leaderboard.get(0).position()).isEqualTo(1);
        assertThat(leaderboard.get(1).points()).isEqualTo(15);
    }

    @Test
    void getLeaderboard_breaksTiesAlphabetically() {
        when(documentRepository.countByAuthorSince(any())).thenReturn(rows(new Object[]{1L, 2L}));
        when(attendanceRepository.countCheckInsByUserSince(any())).thenReturn(List.of());
        when(sessionRepository.countAttendedByCreatorSince(any())).thenReturn(List.of());
        when(reviewRepository.countByAuthorSince(any())).thenReturn(rows(new Object[]{2L, 5L}));
        when(userRepository.findAllById(anyIterable())).thenReturn(List.of(alice, bob));

        assertThat(gamificationService.getLeaderboard("week"))
                .extracting(LeaderboardEntryDTO::username).containsExactly("alice", "bob");
    }

    @Test
    void getLeaderboard_withoutActivity_returnsEmpty() {
        when(documentRepository.countByAuthorSince(any())).thenReturn(List.of());
        when(reviewRepository.countByAuthorSince(any())).thenReturn(List.of());
        when(sessionRepository.countAttendedByCreatorSince(any())).thenReturn(List.of());
        when(attendanceRepository.countCheckInsByUserSince(any())).thenReturn(List.of());

        assertThat(gamificationService.getLeaderboard("week")).isEmpty();
    }

    @Test
    void syncBadges_notifiesOnlyNewlyEarnedBadges() {
        alice.getEarnedBadges().add("FIRST_UPLOAD");
        when(userRepository.findById(1L)).thenReturn(Optional.of(alice));
        when(documentRepository.countByAuthorId(1L)).thenReturn(1L);
        when(reviewRepository.countByAuthorId(1L)).thenReturn(5L);

        gamificationService.syncBadges(1L);
        gamificationService.syncBadges(1L);

        org.mockito.Mockito.verify(notificationService, org.mockito.Mockito.times(1))
                .notifyBadge(org.mockito.ArgumentMatchers.eq(alice), org.mockito.ArgumentMatchers.eq("Crítico"), any());
        assertThat(alice.getEarnedBadges()).containsExactlyInAnyOrder("FIRST_UPLOAD", "CRITIC");
        org.mockito.Mockito.verify(userRepository, org.mockito.Mockito.times(1)).save(alice);
    }

    @Test
    void syncBadges_unknownUser_doesNothing() {
        when(userRepository.findById(9L)).thenReturn(Optional.empty());

        gamificationService.syncBadges(9L);

        org.mockito.Mockito.verifyNoInteractions(notificationService);
    }

    @Test
    void getProfile_noShowsSubtractButNeverBelowZero() {
        when(documentRepository.countByAuthorSince(any())).thenReturn(List.of());
        when(reviewRepository.countByAuthorSince(any())).thenReturn(List.of());
        when(sessionRepository.countAttendedByCreatorSince(any())).thenReturn(List.of());
        when(attendanceRepository.countCheckInsByUserSince(any())).thenReturn(List.of());
        when(attendanceRepository.countNoShowsByUserSince(any(), any())).thenReturn(rows(new Object[]{1L, 2L}));
        when(attendanceRepository.countConfirmedByUserId(1L)).thenReturn(2L);
        when(attendanceRepository.countNoShowsByUserId(org.mockito.ArgumentMatchers.eq(1L), any())).thenReturn(2L);

        GamificationProfileDTO profile = gamificationService.getProfile("alice");

        assertThat(profile.points()).isZero();
        assertThat(profile.weeklyPoints()).isZero();
        assertThat(profile.stats().noShows()).isEqualTo(2);
    }

    @Test
    void getProfile_rewardsImpactWithCappedUpvotes() {
        when(documentRepository.countByAuthorSince(any())).thenReturn(List.of());
        when(reviewRepository.countByAuthorSince(any())).thenReturn(List.of());
        when(sessionRepository.countAttendedByCreatorSince(any())).thenReturn(List.of());
        when(attendanceRepository.countCheckInsByUserSince(any())).thenReturn(List.of());
        when(reviewRepository.countWellRatedByDocumentAuthorSince(any())).thenReturn(rows(new Object[]{1L, 3L}));
        when(forumAnswerRepository.countAcceptedByAuthorSince(any())).thenReturn(rows(new Object[]{1L, 2L}));
        when(forumVoteRepository.countUpvotesPerAnswerSince(any()))
                .thenReturn(rows(new Object[]{1L, 7L}, new Object[]{1L, 2L}, new Object[]{2L, 1L}));

        GamificationProfileDTO profile = gamificationService.getProfile("alice");

        assertThat(profile.points()).isEqualTo(3 * 3 + 2 * 8 + 5 + 2);
        assertThat(profile.rank()).isEqualTo(1);
    }

    @Test
    void getProfileById_usesSameCalculation() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(alice));

        assertThat(gamificationService.getProfile(1L).badges()).hasSize(7);
        when(userRepository.findById(9L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> gamificationService.getProfile(9L))
                .isInstanceOf(com.eduquest.api.exception.ResourceNotFoundException.class);
    }
}
