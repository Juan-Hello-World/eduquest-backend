package com.eduquest.api.service.impl;

import com.eduquest.api.dto.response.ConversationResponseDTO;
import com.eduquest.api.dto.response.DirectMessageResponseDTO;
import com.eduquest.api.entity.DirectMessage;
import com.eduquest.api.entity.User;
import com.eduquest.api.event.OnDirectMessageSentEvent;
import com.eduquest.api.exception.InvalidOperationException;
import com.eduquest.api.exception.ResourceNotFoundException;
import com.eduquest.api.repository.DirectMessageRepository;
import com.eduquest.api.repository.UserRepository;
import com.eduquest.api.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DirectMessageServiceImplTest {

    @Mock DirectMessageRepository messageRepository;
    @Mock UserRepository userRepository;
    @Mock NotificationService notificationService;
    @Mock ApplicationEventPublisher eventPublisher;

    @InjectMocks DirectMessageServiceImpl messageService;

    private User alice;
    private User bob;
    private User carol;

    @BeforeEach
    void setUp() {
        alice = user(1L, "alice");
        bob = user(2L, "bob");
        carol = user(3L, "carol");
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(alice));
        when(userRepository.findById(1L)).thenReturn(Optional.of(alice));
        when(userRepository.findById(2L)).thenReturn(Optional.of(bob));
        when(userRepository.findById(3L)).thenReturn(Optional.of(carol));
    }

    private static User user(Long id, String username) {
        User user = new User();
        user.setId(id);
        user.setUsername(username);
        return user;
    }

    private static DirectMessage message(long id, User sender, User recipient, LocalDateTime at) {
        DirectMessage message = new DirectMessage();
        message.setId(id);
        message.setSender(sender);
        message.setRecipient(recipient);
        message.setContent("Mensaje " + id);
        message.setCreatedAt(at);
        return message;
    }

    @Test
    void getConversations_sortsByLastMessageAndCountsUnread() {
        LocalDateTime now = LocalDateTime.now();
        when(messageRepository.findPartnerIds(1L)).thenReturn(List.of(2L, 3L, 99L));
        when(messageRepository.findLatest(eq(1L), eq(2L), any())).thenReturn(List.of(message(5, bob, alice, now.minusHours(2))));
        when(messageRepository.findLatest(eq(1L), eq(3L), any())).thenReturn(List.of(message(9, alice, carol, now)));
        when(messageRepository.countBySenderIdAndRecipientIdAndReadAtIsNull(2L, 1L)).thenReturn(1L);

        List<ConversationResponseDTO> conversations = messageService.getConversations("alice");

        assertThat(conversations).extracting(c -> c.partner().username()).containsExactly("carol", "bob");
        assertThat(conversations.get(0).lastMessageMine()).isTrue();
        assertThat(conversations.get(1).unreadCount()).isEqualTo(1);
    }

    @Test
    void getMessages_supportsLatestBeforeAndAfter() {
        LocalDateTime now = LocalDateTime.now();
        when(messageRepository.findLatest(eq(1L), eq(2L), any()))
                .thenReturn(List.of(message(6, bob, alice, now), message(5, alice, bob, now)));
        when(messageRepository.findBefore(eq(1L), eq(2L), eq(5L), any())).thenReturn(List.of(message(4, bob, alice, now)));
        when(messageRepository.findAfter(eq(1L), eq(2L), eq(6L), any())).thenReturn(List.of(message(7, bob, alice, now)));

        List<DirectMessageResponseDTO> latest = messageService.getMessages("alice", 2L, null, null);
        assertThat(latest).extracting(DirectMessageResponseDTO::id).containsExactly(5L, 6L);
        assertThat(latest.get(0).mine()).isTrue();
        assertThat(messageService.getMessages("alice", 2L, null, 5L)).hasSize(1);
        assertThat(messageService.getMessages("alice", 2L, 6L, null)).extracting(DirectMessageResponseDTO::id).containsExactly(7L);
    }

    @Test
    void sendMessage_savesAndPublishesEvent() {
        when(messageRepository.save(any(DirectMessage.class))).thenAnswer(inv -> {
            DirectMessage saved = inv.getArgument(0);
            saved.setId(10L);
            return saved;
        });

        DirectMessageResponseDTO dto = messageService.sendMessage("alice", 2L, "  Hola Bob ");

        assertThat(dto.content()).isEqualTo("Hola Bob");
        assertThat(dto.mine()).isTrue();
        ArgumentCaptor<OnDirectMessageSentEvent> captor = ArgumentCaptor.forClass(OnDirectMessageSentEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue().getRecipientId()).isEqualTo(2L);
    }

    @Test
    void sendMessage_toYourselfOrBannedUser_fails() {
        bob.setBanned(true);

        assertThatThrownBy(() -> messageService.sendMessage("alice", 1L, "Hola"))
                .isInstanceOf(InvalidOperationException.class);
        assertThatThrownBy(() -> messageService.sendMessage("alice", 2L, "Hola"))
                .isInstanceOf(InvalidOperationException.class);
    }

    @Test
    void sendMessage_unknownRecipient_throwsNotFound() {
        when(userRepository.findById(50L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> messageService.sendMessage("alice", 50L, "Hola"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void markReadAndCountUnread() {
        when(messageRepository.countByRecipientIdAndReadAtIsNull(1L)).thenReturn(4L);

        messageService.markRead("alice", 2L);

        verify(messageRepository).markRead(eq(1L), eq(2L), any(LocalDateTime.class));
        verify(notificationService).markConversationRead(1L, 2L);
        assertThat(messageService.countUnread("alice")).isEqualTo(4L);
    }

    @Test
    void searchUsers_requiresTwoCharactersAndEscapesPattern() {
        when(userRepository.searchActive(anyString(), eq(1L), any())).thenReturn(List.of(bob));

        assertThat(messageService.searchUsers("alice", "b")).isEmpty();
        assertThat(messageService.searchUsers("alice", null)).isEmpty();
        assertThat(messageService.searchUsers("alice", "@Bo_")).extracting(u -> u.username()).containsExactly("bob");
        verify(userRepository).searchActive(eq("%bo\\_%"), eq(1L), any());
        assertThat(messageService.getUserSummary(2L).username()).isEqualTo("bob");
    }
}
