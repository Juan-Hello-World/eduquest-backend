package com.eduquest.api.service.impl;

import com.eduquest.api.dto.request.GroupMessageRequestDTO;
import com.eduquest.api.dto.response.GroupMessageResponseDTO;
import com.eduquest.api.entity.GroupMessage;
import com.eduquest.api.entity.PrivateGroup;
import com.eduquest.api.entity.User;
import com.eduquest.api.exception.ForbiddenException;
import com.eduquest.api.exception.ResourceNotFoundException;
import com.eduquest.api.repository.GroupMessageRepository;
import com.eduquest.api.repository.PrivateGroupRepository;
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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class GroupMessageServiceImplTest {

    @Mock GroupMessageRepository messageRepository;
    @Mock PrivateGroupRepository groupRepository;
    @Mock UserRepository userRepository;
    @Mock com.eduquest.api.repository.GroupVisitRepository visitRepository;

    @InjectMocks GroupMessageServiceImpl messageService;

    private User alice;
    private User bob;
    private PrivateGroup group;

    @BeforeEach
    void setUp() {
        alice = new User();
        alice.setId(1L);
        alice.setUsername("alice");
        bob = new User();
        bob.setId(2L);
        bob.setUsername("bob");
        bob.setAdvisor(true);
        group = new PrivateGroup();
        group.setId(1L);
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(alice));
        when(groupRepository.existsByIdAndMembersId(1L, 1L)).thenReturn(true);
    }

    private GroupMessage message(long id, User author) {
        GroupMessage message = new GroupMessage();
        message.setId(id);
        message.setContent("Mensaje " + id);
        message.setAuthor(author);
        message.setGroup(group);
        message.setCreatedAt(LocalDateTime.now());
        return message;
    }

    @Test
    void getMessages_withoutCursor_returnsLatestInChronologicalOrder() {
        when(messageRepository.findTop50ByGroupIdOrderByIdDesc(1L)).thenReturn(List.of(message(3, bob), message(2, alice)));

        List<GroupMessageResponseDTO> messages = messageService.getMessages(1L, "alice", null, null);

        assertThat(messages).extracting(GroupMessageResponseDTO::getId).containsExactly(2L, 3L);
        assertThat(messages.get(0).isMine()).isTrue();
        assertThat(messages.get(1).isAuthorAdvisor()).isTrue();
    }

    @Test
    void getMessages_withAfter_returnsNewerOnes() {
        when(messageRepository.findTop100ByGroupIdAndIdGreaterThanOrderByIdAsc(1L, 5L)).thenReturn(List.of(message(6, bob)));

        assertThat(messageService.getMessages(1L, "alice", 5L, null)).hasSize(1);
    }

    @Test
    void getMessages_withBefore_returnsOlderPage() {
        when(messageRepository.findTop50ByGroupIdAndIdLessThanOrderByIdDesc(1L, 10L))
                .thenReturn(List.of(message(9, bob), message(8, bob)));

        assertThat(messageService.getMessages(1L, "alice", null, 10L))
                .extracting(GroupMessageResponseDTO::getId).containsExactly(8L, 9L);
    }

    @Test
    void getMessages_notMember_throwsForbidden() {
        when(groupRepository.existsByIdAndMembersId(1L, 1L)).thenReturn(false);

        assertThatThrownBy(() -> messageService.getMessages(1L, "alice", null, null))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void sendMessage_trimsAndSaves() {
        when(groupRepository.findById(1L)).thenReturn(Optional.of(group));
        when(messageRepository.save(any(GroupMessage.class))).thenAnswer(inv -> {
            GroupMessage saved = inv.getArgument(0);
            saved.setId(11L);
            return saved;
        });
        GroupMessageRequestDTO request = new GroupMessageRequestDTO();
        request.setContent("  Hola grupo  ");

        GroupMessageResponseDTO dto = messageService.sendMessage(1L, request, "alice");

        assertThat(dto.getId()).isEqualTo(11L);
        assertThat(dto.getContent()).isEqualTo("Hola grupo");
        assertThat(dto.isMine()).isTrue();
    }

    @Test
    void sendMessage_missingGroup_throwsNotFound() {
        when(groupRepository.findById(1L)).thenReturn(Optional.empty());
        GroupMessageRequestDTO request = new GroupMessageRequestDTO();
        request.setContent("Hola");

        assertThatThrownBy(() -> messageService.sendMessage(1L, request, "alice"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void anyOperation_unknownUser_throwsNotFound() {
        assertThatThrownBy(() -> messageService.getMessages(1L, "ghost", null, null))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void markChatSeen_createsOrUpdatesVisit() {
        when(groupRepository.findById(1L)).thenReturn(Optional.of(group));
        when(visitRepository.findByUserIdAndGroupId(1L, 1L)).thenReturn(Optional.empty());

        messageService.markChatSeen(1L, "alice");

        org.mockito.ArgumentCaptor<com.eduquest.api.entity.GroupVisit> captor =
                org.mockito.ArgumentCaptor.forClass(com.eduquest.api.entity.GroupVisit.class);
        org.mockito.Mockito.verify(visitRepository).save(captor.capture());
        assertThat(captor.getValue().getChatSeenAt()).isNotNull();
    }

    @Test
    void markChatSeen_missingGroup_throwsNotFound() {
        when(groupRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> messageService.markChatSeen(1L, "alice"))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
