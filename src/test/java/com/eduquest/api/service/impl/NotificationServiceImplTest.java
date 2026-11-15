package com.eduquest.api.service.impl;

import com.eduquest.api.dto.request.AnnouncementRequestDTO;
import com.eduquest.api.entity.Notification;
import com.eduquest.api.entity.NotificationType;
import com.eduquest.api.entity.PrivateGroup;
import com.eduquest.api.entity.User;
import com.eduquest.api.exception.ResourceNotFoundException;
import com.eduquest.api.repository.NotificationRepository;
import com.eduquest.api.repository.PrivateGroupRepository;
import com.eduquest.api.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class NotificationServiceImplTest {

    @Mock NotificationRepository notificationRepository;
    @Mock UserRepository userRepository;
    @Mock PrivateGroupRepository groupRepository;

    @InjectMocks NotificationServiceImpl notificationService;

    private User alice;
    private User bob;
    private PrivateGroup group;

    @BeforeEach
    void setUp() {
        alice = user(1L, "alice");
        bob = user(2L, "bob");
        group = new PrivateGroup();
        group.setId(5L);
        group.setName("Cálculo");
        group.getMembers().add(alice);
        group.getMembers().add(bob);
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(alice));
        when(userRepository.findById(1L)).thenReturn(Optional.of(alice));
        when(userRepository.findById(2L)).thenReturn(Optional.of(bob));
        when(groupRepository.findById(5L)).thenReturn(Optional.of(group));
    }

    private static User user(Long id, String username) {
        User user = new User();
        user.setId(id);
        user.setUsername(username);
        return user;
    }

    @SuppressWarnings("unchecked")
    private List<Notification> savedBatch() {
        ArgumentCaptor<List<Notification>> captor = ArgumentCaptor.forClass(List.class);
        verify(notificationRepository).saveAll(captor.capture());
        return captor.getValue();
    }

    @Test
    void notifyGroupActivity_notifiesEveryoneButTheActor() {
        notificationService.notifyGroupActivity(5L, 1L, NotificationType.GROUP_SESSION, "Repaso");

        List<Notification> sent = savedBatch();
        assertThat(sent).hasSize(1);
        assertThat(sent.get(0).getRecipient()).isEqualTo(bob);
        assertThat(sent.get(0).getTitle()).isEqualTo("Nueva asesoría en Cálculo");
        assertThat(sent.get(0).getBody()).contains("@alice").contains("Repaso");
        assertThat(sent.get(0).getLink()).isEqualTo("/groups/5?tab=sessions");
    }

    @Test
    @SuppressWarnings("unchecked")
    void notifyGroupActivity_buildsMemberAndDocumentMessages() {
        notificationService.notifyGroupActivity(5L, 2L, NotificationType.GROUP_MEMBER, "bob");
        notificationService.notifyGroupActivity(5L, 2L, NotificationType.GROUP_DOCUMENT, "Apuntes");

        ArgumentCaptor<List<Notification>> captor = ArgumentCaptor.forClass(List.class);
        verify(notificationRepository, org.mockito.Mockito.times(2)).saveAll(captor.capture());
        assertThat(captor.getAllValues().get(0).get(0).getLink()).isEqualTo("/groups/5?tab=members");
        assertThat(captor.getAllValues().get(1).get(0).getTitle()).isEqualTo("Nuevo material en Cálculo");
    }

    @Test
    void notifyGroupActivity_missingGroupOrActor_doesNothing() {
        when(groupRepository.findById(9L)).thenReturn(Optional.empty());

        notificationService.notifyGroupActivity(9L, 1L, NotificationType.GROUP_MEMBER, "x");
        notificationService.notifyGroupActivity(5L, 99L, NotificationType.GROUP_MEMBER, "x");

        verify(notificationRepository, never()).saveAll(anyList());
    }

    @Test
    void notifyDirectMessage_reusesUnreadNotificationFromSameSender() {
        Notification existing = new Notification();
        existing.setRecipient(bob);
        existing.setType(NotificationType.DIRECT_MESSAGE);
        existing.setTitle("Mensaje de @alice");
        existing.setCreatedAt(LocalDateTime.now().minusHours(1));
        when(notificationRepository.findFirstByRecipientIdAndCollapseKeyAndReadFalse(2L, "dm:1"))
                .thenReturn(Optional.of(existing), Optional.empty());

        notificationService.notifyDirectMessage(1L, 2L, "Hola otra vez");
        notificationService.notifyDirectMessage(1L, 2L, "x".repeat(200));

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository, org.mockito.Mockito.times(2)).save(captor.capture());
        assertThat(captor.getAllValues().get(0)).isSameAs(existing);
        assertThat(existing.getBody()).isEqualTo("Hola otra vez");
        Notification created = captor.getAllValues().get(1);
        assertThat(created.getLink()).isEqualTo("/messages/1");
        assertThat(created.getCollapseKey()).isEqualTo("dm:1");
        assertThat(created.getBody()).hasSize(120).endsWith("…");
    }

    @Test
    void notifyDirectMessage_unknownUsers_doesNothing() {
        notificationService.notifyDirectMessage(1L, 99L, "Hola");

        verify(notificationRepository, never()).save(any());
    }

    @Test
    void notifyBadge_savesAchievementNotification() {
        notificationService.notifyBadge(alice, "Crítico", "Deja 5 reseñas");

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());
        assertThat(captor.getValue().getType()).isEqualTo(NotificationType.BADGE);
        assertThat(captor.getValue().getLink()).isEqualTo("/achievements");
    }

    @Test
    void announce_toEveryoneOrToOneUser() {
        when(userRepository.findActive()).thenReturn(List.of(alice, bob));
        when(userRepository.findByUsername("bob")).thenReturn(Optional.of(bob));
        AnnouncementRequestDTO request = new AnnouncementRequestDTO();
        request.setTitle(" Mantenimiento ");
        request.setMessage(" La plataforma se actualizará hoy ");

        assertThat(notificationService.announce(request)).isEqualTo(2);

        request.setUsername("@bob");
        assertThat(notificationService.announce(request)).isEqualTo(1);

        request.setUsername("nadie");
        when(userRepository.findByUsername("nadie")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> notificationService.announce(request)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void listCountAndMarkRead_useCurrentUser() {
        Pageable pageable = PageRequest.of(0, 15);
        Notification notification = new Notification();
        notification.setId(7L);
        notification.setType(NotificationType.ANNOUNCEMENT);
        notification.setTitle("Aviso");
        notification.setCreatedAt(LocalDateTime.now());
        when(notificationRepository.findByRecipientIdOrderByCreatedAtDescIdDesc(1L, pageable))
                .thenReturn(new PageImpl<>(List.of(notification), pageable, 1));
        when(notificationRepository.countByRecipientIdAndReadFalse(1L)).thenReturn(3L);
        when(notificationRepository.findByIdAndRecipientId(7L, 1L)).thenReturn(Optional.of(notification));
        when(notificationRepository.findByIdAndRecipientId(8L, 1L)).thenReturn(Optional.empty());

        assertThat(notificationService.getNotifications("alice", pageable).content())
                .extracting(dto -> dto.type()).containsExactly("ANNOUNCEMENT");
        assertThat(notificationService.countUnread("alice")).isEqualTo(3L);
        notificationService.markRead(7L, "alice");
        assertThat(notification.isRead()).isTrue();
        assertThatThrownBy(() -> notificationService.markRead(8L, "alice")).isInstanceOf(ResourceNotFoundException.class);
        notificationService.markAllRead("alice");
        verify(notificationRepository).markAllRead(1L);
        notificationService.markConversationRead(1L, 2L);
        verify(notificationRepository).markReadByCollapseKey(1L, "dm:2");
    }

    @Test
    void unknownUser_throwsNotFound() {
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> notificationService.countUnread("ghost")).isInstanceOf(ResourceNotFoundException.class);
    }
}
