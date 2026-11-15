package com.eduquest.api.controller;

import com.eduquest.api.dto.request.GroupMessageRequestDTO;
import com.eduquest.api.dto.request.ReportRequestDTO;
import com.eduquest.api.dto.request.ReportStatusRequestDTO;
import com.eduquest.api.dto.response.GamificationProfileDTO;
import com.eduquest.api.dto.response.GamificationStatsDTO;
import com.eduquest.api.dto.response.GroupMessageResponseDTO;
import com.eduquest.api.dto.response.LeaderboardEntryDTO;
import com.eduquest.api.dto.response.PageResponseDTO;
import com.eduquest.api.dto.response.ReportResponseDTO;
import com.eduquest.api.service.DocumentService;
import com.eduquest.api.service.GamificationService;
import com.eduquest.api.service.GroupMessageService;
import com.eduquest.api.service.ReportService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CommunityControllersTest {

    @Mock GroupMessageService messageService;
    @Mock ReportService reportService;
    @Mock DocumentService documentService;
    @Mock GamificationService gamificationService;
    @Mock com.eduquest.api.service.NotificationService notificationService;
    @Mock com.eduquest.api.service.DirectMessageService directMessageService;

    private Authentication authentication;

    @BeforeEach
    void setUp() {
        authentication = mock(Authentication.class);
        when(authentication.getName()).thenReturn("alice");
    }

    @Test
    void groupMessageController_listsAndSends() {
        GroupMessageController controller = new GroupMessageController(messageService);
        when(messageService.getMessages(1L, "alice", 4L, null)).thenReturn(List.of(new GroupMessageResponseDTO()));
        when(messageService.sendMessage(eq(1L), any(GroupMessageRequestDTO.class), eq("alice")))
                .thenReturn(new GroupMessageResponseDTO());

        assertThat(controller.getMessages(1L, 4L, null, authentication).getBody()).hasSize(1);
        assertThat(controller.sendMessage(1L, new GroupMessageRequestDTO(), authentication).getStatusCode())
                .isEqualTo(HttpStatus.CREATED);
    }

    @Test
    void reportController_createsReport() {
        ReportController controller = new ReportController(reportService);
        when(reportService.createReport(any(ReportRequestDTO.class), eq("alice"))).thenReturn(new ReportResponseDTO());

        assertThat(controller.createReport(new ReportRequestDTO(), authentication).getStatusCode())
                .isEqualTo(HttpStatus.CREATED);
    }

    @Test
    void moderationController_listsCountsClosesAndDeletes() {
        ModerationController controller = new ModerationController(reportService, documentService, notificationService);
        when(reportService.getReports(eq("OPEN"), any(Pageable.class)))
                .thenReturn(new PageResponseDTO<>(List.of(new ReportResponseDTO()), 0, 10, 1, 1));
        when(reportService.countOpenReports()).thenReturn(3L);
        when(reportService.updateStatus(5L, "RESOLVED", "alice")).thenReturn(new ReportResponseDTO());
        ReportStatusRequestDTO request = new ReportStatusRequestDTO();
        request.setStatus("RESOLVED");

        assertThat(controller.getReports("OPEN", -1, 500).getBody().content()).hasSize(1);
        assertThat(controller.countOpenReports().getBody()).containsEntry("count", 3L);
        assertThat(controller.updateReport(5L, request, authentication).getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(controller.deleteDocument(8L).getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(documentService).deleteDocument(8L);
    }

    @Test
    void gamificationController_returnsProfileAndLeaderboard() {
        GamificationController controller = new GamificationController(gamificationService);
        GamificationProfileDTO profile = new GamificationProfileDTO(10, 5, 1,
                new GamificationStatsDTO(1, 0, 0, 0, 0), List.of());
        when(gamificationService.getProfile("alice")).thenReturn(profile);
        when(gamificationService.getLeaderboard("week"))
                .thenReturn(List.of(new LeaderboardEntryDTO(1, 1L, "alice", null, false, 10)));

        assertThat(controller.getMyProfile(authentication).getBody().points()).isEqualTo(10);
        assertThat(controller.getLeaderboard("week").getBody()).hasSize(1);
    }

    @Test
    void moderationController_announce_returnsRecipients() {
        ModerationController controller = new ModerationController(reportService, documentService, notificationService);
        com.eduquest.api.dto.request.AnnouncementRequestDTO request = new com.eduquest.api.dto.request.AnnouncementRequestDTO();
        when(notificationService.announce(request)).thenReturn(4);

        assertThat(controller.announce(request).getBody()).containsEntry("recipients", 4);
    }

    @Test
    void notificationController_listsCountsAndMarksRead() {
        NotificationController controller = new NotificationController(notificationService);
        when(notificationService.getNotifications(eq("alice"), any(Pageable.class)))
                .thenReturn(new PageResponseDTO<>(List.of(), 0, 15, 0, 0));
        when(notificationService.countUnread("alice")).thenReturn(2L);

        assertThat(controller.getNotifications(-1, 99, authentication).getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(controller.countUnread(authentication).getBody()).containsEntry("count", 2L);
        assertThat(controller.markRead(3L, authentication).getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(controller.markAllRead(authentication).getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(notificationService).markRead(3L, "alice");
        verify(notificationService).markAllRead("alice");
    }

    @Test
    void directMessageController_delegatesToService() {
        DirectMessageController controller = new DirectMessageController(directMessageService);
        com.eduquest.api.dto.response.UserSummaryDTO bob = new com.eduquest.api.dto.response.UserSummaryDTO(2L, "bob", null, false);
        when(directMessageService.getConversations("alice")).thenReturn(List.of());
        when(directMessageService.countUnread("alice")).thenReturn(1L);
        when(directMessageService.searchUsers("alice", "bo")).thenReturn(List.of(bob));
        when(directMessageService.getUserSummary(2L)).thenReturn(bob);
        when(directMessageService.getMessages("alice", 2L, null, null)).thenReturn(List.of());
        GroupMessageRequestDTO request = new GroupMessageRequestDTO();
        request.setContent("Hola");
        when(directMessageService.sendMessage("alice", 2L, "Hola")).thenReturn(
                new com.eduquest.api.dto.response.DirectMessageResponseDTO(1L, "Hola", 1L, true, false, java.time.LocalDateTime.now()));

        assertThat(controller.getConversations(authentication).getBody()).isEmpty();
        assertThat(controller.countUnread(authentication).getBody()).containsEntry("count", 1L);
        assertThat(controller.searchUsers("bo", authentication).getBody()).containsExactly(bob);
        assertThat(controller.getUser(2L).getBody()).isEqualTo(bob);
        assertThat(controller.getMessages(2L, null, null, authentication).getBody()).isEmpty();
        assertThat(controller.sendMessage(2L, request, authentication).getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(controller.markRead(2L, authentication).getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(directMessageService).markRead("alice", 2L);
    }

    @Test
    void groupMessageController_markSeen() {
        GroupMessageController controller = new GroupMessageController(messageService);

        assertThat(controller.markSeen(1L, authentication).getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(messageService).markChatSeen(1L, "alice");
    }
}
