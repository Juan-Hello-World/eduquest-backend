package com.eduquest.api.service.impl;

import com.eduquest.api.dto.request.GroupMessageRequestDTO;
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
    @Mock com.eduquest.api.service.FileStorageService fileStorageService;

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

    private OnDirectMessageSentEvent sentEvent() {
        ArgumentCaptor<org.springframework.context.ApplicationEvent> captor =
                ArgumentCaptor.forClass(org.springframework.context.ApplicationEvent.class);
        verify(eventPublisher, org.mockito.Mockito.atLeastOnce()).publishEvent(captor.capture());
        return captor.getAllValues().stream()
                .filter(OnDirectMessageSentEvent.class::isInstance)
                .map(OnDirectMessageSentEvent.class::cast)
                .findFirst()
                .orElseThrow();
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

        DirectMessageResponseDTO dto = messageService.sendMessage("alice", 2L, request("  Hola Bob ", null, null));

        assertThat(dto.content()).isEqualTo("Hola Bob");
        assertThat(dto.mine()).isTrue();
        OnDirectMessageSentEvent sent = sentEvent();
        assertThat(sent.getRecipientId()).isEqualTo(2L);
    }

    @Test
    void sendMessage_toYourselfOrBannedUser_fails() {
        bob.setBanned(true);

        assertThatThrownBy(() -> messageService.sendMessage("alice", 1L, request("Hola", null, null)))
                .isInstanceOf(InvalidOperationException.class);
        assertThatThrownBy(() -> messageService.sendMessage("alice", 2L, request("Hola", null, null)))
                .isInstanceOf(InvalidOperationException.class);
    }

    @Test
    void sendMessage_unknownRecipient_throwsNotFound() {
        when(userRepository.findById(50L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> messageService.sendMessage("alice", 50L, request("Hola", null, null)))
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

    @Test
    void sendMessage_imageOnly_usesImagePreview() {
        when(messageRepository.save(any(DirectMessage.class))).thenAnswer(inv -> inv.getArgument(0));
        String image = "http://localhost:8080/api/v1/files/0f8c6a43-6a1e-4c55-9a6f-9a7d5a1b2c3d/foto.png";

        DirectMessageResponseDTO dto = messageService.sendMessage("alice", 2L, request("  ", image, null));

        assertThat(dto.content()).isEmpty();
        assertThat(dto.imageUrl()).isEqualTo(image);
        OnDirectMessageSentEvent sent = sentEvent();
        assertThat(sent.getPreview()).isEqualTo("Envió una imagen");
        assertThatThrownBy(() -> messageService.sendMessage("alice", 2L, request(" ", " ", null)))
                .isInstanceOf(com.eduquest.api.exception.BadRequestException.class);
    }

    @Test
    void sendMessage_asReply_includesReplyPreviewAndBroadcasts() {
        DirectMessage original = message(5L, bob, alice, LocalDateTime.now());
        when(messageRepository.findById(5L)).thenReturn(Optional.of(original));
        when(messageRepository.save(any(DirectMessage.class))).thenAnswer(invocation -> {
            DirectMessage saved = invocation.getArgument(0);
            saved.setId(6L);
            return saved;
        });

        DirectMessageResponseDTO dto = messageService.sendMessage("alice", 2L, request("Respuesta", null, 5L));

        assertThat(dto.replyTo().id()).isEqualTo(5L);
        assertThat(dto.replyTo().authorUsername()).isEqualTo("bob");
        verify(eventPublisher).publishEvent(org.mockito.ArgumentMatchers.isA(com.eduquest.api.realtime.RealtimeEvent.class));
    }

    @Test
    void editMessage_ownMessage_updatesContent_othersForbidden() {
        DirectMessage mine = message(7L, alice, bob, LocalDateTime.now());
        DirectMessage theirs = message(8L, bob, alice, LocalDateTime.now());
        when(messageRepository.findById(7L)).thenReturn(Optional.of(mine));
        when(messageRepository.findById(8L)).thenReturn(Optional.of(theirs));
        when(messageRepository.save(any(DirectMessage.class))).thenAnswer(invocation -> invocation.getArgument(0));

        DirectMessageResponseDTO dto = messageService.editMessage("alice", 2L, 7L, "  Corregido ");

        assertThat(dto.content()).isEqualTo("Corregido");
        assertThat(dto.editedAt()).isNotNull();
        assertThatThrownBy(() -> messageService.editMessage("alice", 2L, 8L, "x"))
                .isInstanceOf(com.eduquest.api.exception.ForbiddenException.class);
        assertThatThrownBy(() -> messageService.editMessage("alice", 3L, 7L, "x"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void deleteMessage_hidesContentAndRemovesImage() {
        DirectMessage mine = message(9L, alice, bob, LocalDateTime.now());
        mine.setImageUrl("http://localhost:8080/api/v1/files/0c5b92b4-9d12-440a-847e-af7c2902b2e8/foto.png");
        when(messageRepository.findById(9L)).thenReturn(Optional.of(mine));
        when(messageRepository.save(any(DirectMessage.class))).thenAnswer(invocation -> invocation.getArgument(0));

        DirectMessageResponseDTO dto = messageService.deleteMessage("alice", 2L, 9L);

        assertThat(dto.deleted()).isTrue();
        assertThat(dto.content()).isEmpty();
        assertThat(dto.imageUrl()).isNull();
        verify(fileStorageService).deleteByUrl(org.mockito.ArgumentMatchers.contains("/api/v1/files/"));
        assertThatThrownBy(() -> messageService.editMessage("alice", 2L, 9L, "x"))
                .isInstanceOf(com.eduquest.api.exception.BadRequestException.class);
        assertThat(messageService.deleteMessage("alice", 2L, 9L).deleted()).isTrue();
        verify(messageRepository, org.mockito.Mockito.times(1)).save(mine);

        DirectMessage theirs = message(10L, bob, alice, LocalDateTime.now());
        when(messageRepository.findById(10L)).thenReturn(Optional.of(theirs));
        assertThatThrownBy(() -> messageService.deleteMessage("alice", 2L, 10L))
                .isInstanceOf(com.eduquest.api.exception.ForbiddenException.class);
    }

    private static GroupMessageRequestDTO request(String content, String imageUrl, Long replyToId) {
        GroupMessageRequestDTO request = new GroupMessageRequestDTO();
        request.setContent(content);
        request.setImageUrl(imageUrl);
        request.setReplyToId(replyToId);
        return request;
    }

    @Test
    void sendMessage_withFileOrSticker_needsNoText() {
        when(messageRepository.save(any(DirectMessage.class))).thenAnswer(invocation -> invocation.getArgument(0));
        GroupMessageRequestDTO file = request(null, null, null);
        file.setFileUrl("http://localhost:8080/api/v1/files/0f8c6a43-6a1e-4c55-9a6f-9a7d5a1b2c3d/notas.docx");
        GroupMessageRequestDTO sticker = request(" ", null, null);
        sticker.setSticker("pack:gracias");

        DirectMessageResponseDTO withFile = messageService.sendMessage("alice", 2L, file);
        DirectMessageResponseDTO withSticker = messageService.sendMessage("alice", 2L, sticker);

        assertThat(withFile.fileName()).isEqualTo("notas.docx");
        assertThat(withFile.content()).isEmpty();
        assertThat(withSticker.sticker()).isEqualTo("pack:gracias");
    }
}
