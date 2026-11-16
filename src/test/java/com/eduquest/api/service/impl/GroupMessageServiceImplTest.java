package com.eduquest.api.service.impl;

import com.eduquest.api.dto.request.GroupMessageRequestDTO;
import com.eduquest.api.dto.request.PollRequestDTO;
import com.eduquest.api.dto.response.ChannelSummaryDTO;
import com.eduquest.api.dto.response.GroupMessageResponseDTO;
import com.eduquest.api.dto.response.ThreadResponseDTO;
import com.eduquest.api.entity.GroupChannel;
import com.eduquest.api.entity.GroupMessage;
import com.eduquest.api.entity.GroupVisit;
import com.eduquest.api.entity.MessageReaction;
import com.eduquest.api.entity.Poll;
import com.eduquest.api.entity.PollOption;
import com.eduquest.api.entity.PollVote;
import com.eduquest.api.entity.PrivateGroup;
import com.eduquest.api.entity.Role;
import com.eduquest.api.entity.RoleName;
import com.eduquest.api.entity.SavedMessage;
import com.eduquest.api.entity.User;
import com.eduquest.api.exception.BadRequestException;
import com.eduquest.api.exception.ForbiddenException;
import com.eduquest.api.exception.ResourceNotFoundException;
import com.eduquest.api.realtime.RealtimeEvent;
import com.eduquest.api.repository.GroupMessageRepository;
import com.eduquest.api.repository.GroupVisitRepository;
import com.eduquest.api.repository.MessageReactionRepository;
import com.eduquest.api.repository.PollRepository;
import com.eduquest.api.repository.PollVoteRepository;
import com.eduquest.api.repository.PrivateGroupRepository;
import com.eduquest.api.repository.UserRepository;
import com.eduquest.api.service.FileStorageService;
import com.eduquest.api.service.GroupCommunityService;
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
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class GroupMessageServiceImplTest {

    private static final String IMAGE = "http://localhost:8080/api/v1/files/0f8c6a43-6a1e-4c55-9a6f-9a7d5a1b2c3d/foto.png";
    private static final String FILE = "http://localhost:8080/api/v1/files/0f8c6a43-6a1e-4c55-9a6f-9a7d5a1b2c3d/Gu%C3%ADa%20final.pdf";

    @Mock GroupMessageRepository messageRepository;
    @Mock PrivateGroupRepository groupRepository;
    @Mock UserRepository userRepository;
    @Mock GroupVisitRepository visitRepository;
    @Mock FileStorageService fileStorageService;
    @Mock ApplicationEventPublisher eventPublisher;
    @Mock GroupCommunityService communityService;
    @Mock MessageReactionRepository reactionRepository;
    @Mock PollRepository pollRepository;
    @Mock PollVoteRepository pollVoteRepository;
    @Mock NotificationService notificationService;
    @Mock com.eduquest.api.repository.SavedMessageRepository savedMessageRepository;
    @Mock com.eduquest.api.repository.ChannelReadRepository channelReadRepository;
    @Mock GeminiClient geminiClient;

    @InjectMocks GroupMessageServiceImpl messageService;

    private User alice;
    private User bob;
    private PrivateGroup group;
    private GroupChannel general;

    @BeforeEach
    void setUp() {
        alice = user(1L, "alice");
        bob = user(2L, "bob");
        bob.setAdvisor(true);
        group = new PrivateGroup();
        group.setId(1L);
        group.setName("Cálculo");
        group.setCreator(bob);
        group.getMembers().add(alice);
        group.getMembers().add(bob);
        general = new GroupChannel();
        general.setId(4L);
        general.setName("general");
        general.setGroup(group);
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(alice));
        when(userRepository.findByUsername("bob")).thenReturn(Optional.of(bob));
        when(groupRepository.existsByIdAndMembersId(eq(1L), any())).thenReturn(true);
        when(groupRepository.findById(1L)).thenReturn(Optional.of(group));
        when(groupRepository.findMemberIds(1L)).thenReturn(List.of(1L, 2L));
        when(communityService.resolveChannel(eq(group), any())).thenReturn(general);
        when(communityService.resolveChannel(eq(group), isNull())).thenReturn(general);
        when(messageRepository.save(any(GroupMessage.class))).thenAnswer(invocation -> {
            GroupMessage saved = invocation.getArgument(0);
            if (saved.getId() == null) {
                saved.setId(50L);
            }
            return saved;
        });
    }

    private static User user(Long id, String username) {
        User user = new User();
        user.setId(id);
        user.setUsername(username);
        return user;
    }

    private GroupMessage message(long id, User author) {
        GroupMessage message = new GroupMessage();
        message.setId(id);
        message.setContent("Mensaje " + id);
        message.setAuthor(author);
        message.setGroup(group);
        message.setChannel(general);
        message.setCreatedAt(LocalDateTime.now());
        when(messageRepository.findById(id)).thenReturn(Optional.of(message));
        return message;
    }

    private static GroupMessageRequestDTO text(String content) {
        GroupMessageRequestDTO request = new GroupMessageRequestDTO();
        request.setContent(content);
        return request;
    }

    private RealtimeEvent lastEvent() {
        ArgumentCaptor<ApplicationEvent> captor = ArgumentCaptor.forClass(ApplicationEvent.class);
        verify(eventPublisher, org.mockito.Mockito.atLeastOnce()).publishEvent(captor.capture());
        return (RealtimeEvent) captor.getValue();
    }

    @Test
    void getMessages_withoutCursor_returnsLatestOfChannelInChronologicalOrder() {
        GroupMessage latest = message(3, bob);
        GroupMessage previous = message(2, alice);
        when(messageRepository.findTop50ByChannelIdAndThreadRootIsNullOrderByIdDesc(4L)).thenReturn(List.of(latest, previous));

        List<GroupMessageResponseDTO> messages = messageService.getMessages(1L, "alice", null, null, null, null);

        assertThat(messages).extracting(GroupMessageResponseDTO::getId).containsExactly(2L, 3L);
        assertThat(messages.get(0).isMine()).isTrue();
        assertThat(messages.get(1).isAuthorAdvisor()).isTrue();
        assertThat(messages.get(0).getChannelId()).isEqualTo(4L);
    }

    @Test
    void getMessages_withCursors_usesTheRightQuery() {
        GroupMessage newer = message(6, bob);
        GroupMessage older = message(9, bob);
        GroupMessage oldest = message(8, bob);
        when(messageRepository.findTop100ByChannelIdAndThreadRootIsNullAndIdGreaterThanOrderByIdAsc(4L, 5L))
                .thenReturn(List.of(newer));
        when(messageRepository.findTop50ByChannelIdAndThreadRootIsNullAndIdLessThanOrderByIdDesc(4L, 10L))
                .thenReturn(List.of(older, oldest));

        assertThat(messageService.getMessages(1L, "alice", 4L, 5L, null, null)).hasSize(1);
        assertThat(messageService.getMessages(1L, "alice", 4L, null, 10L, null))
                .extracting(GroupMessageResponseDTO::getId).containsExactly(8L, 9L);
    }

    @Test
    void getMessages_includesReactionsThreadCountsAndPolls() {
        GroupMessage first = message(2, alice);
        Poll poll = poll(30L, "¿Repaso?", "Sí", "No");
        first.setPoll(poll);
        PollVote vote = new PollVote();
        vote.setPoll(poll);
        vote.setOption(poll.getOptions().getFirst());
        vote.setUser(bob);
        when(messageRepository.findTop50ByChannelIdAndThreadRootIsNullOrderByIdDesc(4L)).thenReturn(List.of(first));
        when(reactionRepository.findByMessageIdIn(anyList()))
                .thenReturn(List.of(reaction(1L, first, alice, "👍"), reaction(2L, first, bob, "👍"),
                        reaction(3L, first, bob, "🔥")));
        List<Object[]> counts = new ArrayList<>();
        counts.add(new Object[]{2L, 3L});
        when(messageRepository.countThreadReplies(anyList())).thenReturn(counts);
        when(pollVoteRepository.findByPollIdIn(anyList())).thenReturn(List.of(vote));

        GroupMessageResponseDTO dto = messageService.getMessages(1L, "alice", null, null, null, null).getFirst();

        assertThat(dto.getThreadReplyCount()).isEqualTo(3L);
        assertThat(dto.getReactions()).hasSize(2);
        assertThat(dto.getReactions().getFirst().emoji()).isEqualTo("👍");
        assertThat(dto.getReactions().getFirst().count()).isEqualTo(2);
        assertThat(dto.getReactions().getFirst().userIds()).containsExactly(1L, 2L);
        assertThat(dto.getPoll().totalVotes()).isEqualTo(1);
        assertThat(dto.getPoll().options().getFirst().voterIds()).containsExactly(2L);
        assertThat(dto.getPoll().options().get(1).votes()).isZero();
    }

    @Test
    void getMessages_notMemberOrUnknownUser_isRejected() {
        when(groupRepository.existsByIdAndMembersId(1L, 1L)).thenReturn(false);

        assertThatThrownBy(() -> messageService.getMessages(1L, "alice", null, null, null, null))
                .isInstanceOf(ForbiddenException.class);
        assertThatThrownBy(() -> messageService.getMessages(1L, "ghost", null, null, null, null))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void sendMessage_trimsSavesAndBroadcasts() {
        GroupMessageResponseDTO dto = messageService.sendMessage(1L, text("  Hola grupo  "), "alice");

        assertThat(dto.getId()).isEqualTo(50L);
        assertThat(dto.getContent()).isEqualTo("Hola grupo");
        assertThat(dto.isMine()).isTrue();
        assertThat(dto.getChannelId()).isEqualTo(4L);
        RealtimeEvent event = lastEvent();
        assertThat(event.getUserIds()).containsExactly(1L, 2L);
        assertThat(event.getPayload()).containsEntry("action", "created").containsEntry("channelId", 4L);
    }

    @Test
    void sendMessage_missingGroup_throwsNotFound() {
        when(groupRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> messageService.sendMessage(1L, text("Hola"), "alice"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void sendMessage_withAttachmentsOnly_keepsThem() {
        GroupMessageRequestDTO image = new GroupMessageRequestDTO();
        image.setImageUrl(IMAGE);
        GroupMessageRequestDTO file = new GroupMessageRequestDTO();
        file.setFileUrl(FILE);
        GroupMessageRequestDTO sticker = new GroupMessageRequestDTO();
        sticker.setSticker("pack:bravo");

        assertThat(messageService.sendMessage(1L, image, "alice").getImageUrl()).endsWith("foto.png");
        GroupMessageResponseDTO withFile = messageService.sendMessage(1L, file, "alice");
        assertThat(withFile.getContent()).isEmpty();
        assertThat(withFile.getFileName()).isEqualTo("Guía final.pdf");
        assertThat(messageService.sendMessage(1L, sticker, "alice").getSticker()).isEqualTo("pack:bravo");
    }

    @Test
    void sendMessage_withoutContent_isRejected() {
        assertThatThrownBy(() -> messageService.sendMessage(1L, new GroupMessageRequestDTO(), "alice"))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void sendMessage_asReply_linksOriginal() {
        message(5, bob);
        GroupMessageRequestDTO request = text("De acuerdo");
        request.setReplyToId(5L);

        GroupMessageResponseDTO dto = messageService.sendMessage(1L, request, "alice");

        assertThat(dto.getReplyTo().authorUsername()).isEqualTo("bob");
        assertThat(dto.getReplyTo().preview()).isEqualTo("Mensaje 5");
    }

    @Test
    void sendMessage_inThread_usesTheRootAndItsChannel() {
        GroupChannel other = new GroupChannel();
        other.setId(8L);
        other.setName("dudas");
        GroupMessage root = message(5, bob);
        root.setChannel(other);
        GroupMessage reply = message(6, bob);
        reply.setThreadRoot(root);
        GroupMessageRequestDTO request = text("Sigo el hilo");
        request.setThreadRootId(6L);

        GroupMessageResponseDTO dto = messageService.sendMessage(1L, request, "alice");

        assertThat(dto.getThreadRootId()).isEqualTo(5L);
        assertThat(dto.getChannelId()).isEqualTo(8L);
    }

    @Test
    void sendMessage_withMentions_notifiesOnlyMentionedMembers() {
        messageService.sendMessage(1L, text("Hola @Bob y @alice, también @nadie"), "alice");

        verify(notificationService).notifyMention(eq(bob), eq(alice), eq(group), eq(general), eq(50L), anyString());
        verify(notificationService, never()).notifyMention(eq(alice), any(), any(), any(), any(), anyString());
    }

    @Test
    void sendMessage_withoutMentions_doesNotNotify() {
        messageService.sendMessage(1L, text("Hola a todos"), "alice");

        verify(notificationService, never()).notifyMention(any(), any(), any(), any(), any(), anyString());
    }

    @Test
    void editMessage_onlyAuthorAndNotDeleted() {
        message(7, alice);
        message(8, bob);

        GroupMessageResponseDTO dto = messageService.editMessage(1L, 7L, " Nuevo texto ", "alice");

        assertThat(dto.getContent()).isEqualTo("Nuevo texto");
        assertThat(dto.getEditedAt()).isNotNull();
        assertThat(lastEvent().getPayload()).containsEntry("action", "updated");
        assertThatThrownBy(() -> messageService.editMessage(1L, 8L, "x", "alice")).isInstanceOf(ForbiddenException.class);
        assertThatThrownBy(() -> messageService.editMessage(1L, 99L, "x", "alice"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void deleteMessage_authorGroupManagerOrPlatformModerator() {
        GroupMessage mine = message(9, alice);
        mine.setImageUrl(IMAGE);
        mine.setFileUrl(FILE);
        mine.setPinned(true);
        message(10, bob);
        message(11, alice);

        GroupMessageResponseDTO dto = messageService.deleteMessage(1L, 9L, "alice");

        assertThat(dto.isDeleted()).isTrue();
        assertThat(dto.getContent()).isEmpty();
        assertThat(dto.getImageUrl()).isNull();
        assertThat(dto.isPinned()).isFalse();
        verify(fileStorageService, times(2)).deleteByUrl(contains("/api/v1/files/"));
        assertThatThrownBy(() -> messageService.editMessage(1L, 9L, "x", "alice")).isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> messageService.deleteMessage(1L, 10L, "alice")).isInstanceOf(ForbiddenException.class);

        assertThat(messageService.deleteMessage(1L, 11L, "bob").isDeleted()).isTrue();
        alice.getRoles().add(new Role(2L, RoleName.ROLE_MANAGER));
        assertThat(messageService.deleteMessage(1L, 10L, "alice").isDeleted()).isTrue();
    }

    @Test
    void toggleReaction_addsAndRemoves() {
        GroupMessage target = message(12, bob);
        MessageReaction existing = reaction(1L, target, alice, "👍");
        when(reactionRepository.findByMessageIdAndUserIdAndEmoji(12L, 1L, "👍"))
                .thenReturn(Optional.empty(), Optional.of(existing));

        messageService.toggleReaction(1L, 12L, " 👍 ", "alice");
        messageService.toggleReaction(1L, 12L, "👍", "alice");

        ArgumentCaptor<MessageReaction> captor = ArgumentCaptor.forClass(MessageReaction.class);
        verify(reactionRepository).save(captor.capture());
        assertThat(captor.getValue().getEmoji()).isEqualTo("👍");
        verify(reactionRepository).delete(existing);
        assertThat(lastEvent().getPayload()).containsEntry("action", "updated");
    }

    @Test
    void toggleReaction_respectsLimitsAndDeletedMessages() {
        GroupMessage target = message(13, bob);
        when(reactionRepository.findByMessageIdAndUserIdAndEmoji(eq(13L), eq(1L), anyString())).thenReturn(Optional.empty());
        when(reactionRepository.countByMessageIdAndUserId(13L, 1L)).thenReturn((long) GroupMessageServiceImpl.MAX_REACTIONS_PER_USER);

        assertThatThrownBy(() -> messageService.toggleReaction(1L, 13L, "😀", "alice"))
                .isInstanceOf(BadRequestException.class);
        target.setDeleted(true);
        assertThatThrownBy(() -> messageService.toggleReaction(1L, 13L, "😀", "alice"))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void setPinned_onlyForGroupManagers() {
        message(14, alice);

        assertThatThrownBy(() -> messageService.setPinned(1L, 14L, true, "alice")).isInstanceOf(ForbiddenException.class);

        GroupMessageResponseDTO pinned = messageService.setPinned(1L, 14L, true, "bob");
        assertThat(pinned.isPinned()).isTrue();
        assertThat(messageService.setPinned(1L, 14L, false, "bob").isPinned()).isFalse();

        group.getModerators().add(alice);
        assertThat(messageService.setPinned(1L, 14L, true, "alice").isPinned()).isTrue();
    }

    @Test
    void setPinned_deletedMessage_isRejected() {
        message(15, alice).setDeleted(true);

        assertThatThrownBy(() -> messageService.setPinned(1L, 15L, true, "bob")).isInstanceOf(BadRequestException.class);
    }

    @Test
    void getPinned_returnsChannelPins() {
        GroupMessage pinned = message(16, bob);
        pinned.setPinned(true);
        when(messageRepository.findByChannelIdAndPinnedTrueOrderByPinnedAtDesc(4L)).thenReturn(List.of(pinned));

        assertThat(messageService.getPinned(1L, null, "alice")).extracting(GroupMessageResponseDTO::getId).containsExactly(16L);
    }

    @Test
    void getThread_resolvesTheRootFromAnyReply() {
        GroupMessage root = message(20, bob);
        GroupMessage reply = message(21, alice);
        reply.setThreadRoot(root);
        when(messageRepository.findByThreadRootIdOrderByIdAsc(20L)).thenReturn(List.of(reply));

        ThreadResponseDTO thread = messageService.getThread(1L, 21L, "alice");

        assertThat(thread.root().getId()).isEqualTo(20L);
        assertThat(thread.replies()).extracting(GroupMessageResponseDTO::getId).containsExactly(21L);
    }

    @Test
    void search_escapesWildcardsAndValidatesLength() {
        GroupMessage found = message(22, bob);
        when(messageRepository.search(eq(1L), isNull(), anyString(), any(Pageable.class))).thenReturn(List.of(found));

        assertThat(messageService.search(1L, null, " 100%_Ok ", "alice")).hasSize(1);
        verify(messageRepository).search(eq(1L), isNull(), eq("%100\\%\\_ok%"), any(Pageable.class));
        assertThatThrownBy(() -> messageService.search(1L, null, " a ", "alice")).isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> messageService.search(1L, null, null, "alice")).isInstanceOf(BadRequestException.class);
    }

    @Test
    void createPoll_removesDuplicatedOptionsAndPostsMessage() {
        when(pollRepository.save(any(Poll.class))).thenAnswer(invocation -> {
            Poll poll = invocation.getArgument(0);
            poll.setId(31L);
            return poll;
        });
        PollRequestDTO request = new PollRequestDTO();
        request.setQuestion(" ¿Cuándo estudiamos? ");
        request.setOptions(List.of("Lunes", " Lunes ", "Martes"));

        GroupMessageResponseDTO dto = messageService.createPoll(1L, request, "alice");

        assertThat(dto.getPoll().question()).isEqualTo("¿Cuándo estudiamos?");
        assertThat(dto.getPoll().options()).extracting(option -> option.text()).containsExactly("Lunes", "Martes");
        assertThat(lastEvent().getPayload()).containsEntry("action", "created");

        request.setOptions(List.of("Igual", "Igual "));
        assertThatThrownBy(() -> messageService.createPoll(1L, request, "alice")).isInstanceOf(BadRequestException.class);
    }

    @Test
    void votePoll_createsChangesAndRemovesVote() {
        Poll poll = poll(32L, "¿Tema?", "Límites", "Derivadas");
        poll.setGroup(group);
        GroupMessage pollMessage = message(23, bob);
        pollMessage.setPoll(poll);
        when(pollRepository.findById(32L)).thenReturn(Optional.of(poll));
        when(messageRepository.findByPollId(32L)).thenReturn(Optional.of(pollMessage));
        PollVote existing = new PollVote();
        existing.setPoll(poll);
        existing.setUser(alice);
        existing.setOption(poll.getOptions().getFirst());
        when(pollVoteRepository.findByPollIdAndUserId(32L, 1L))
                .thenReturn(Optional.empty(), Optional.of(existing), Optional.of(existing));

        messageService.votePoll(1L, 32L, 1L, "alice");
        messageService.votePoll(1L, 32L, 2L, "alice");
        messageService.votePoll(1L, 32L, null, "alice");

        verify(pollVoteRepository, times(2)).save(any(PollVote.class));
        assertThat(existing.getOption().getId()).isEqualTo(2L);
        verify(pollVoteRepository).delete(existing);
        assertThatThrownBy(() -> messageService.votePoll(1L, 32L, 99L, "alice")).isInstanceOf(BadRequestException.class);
    }

    @Test
    void votePoll_fromAnotherGroup_isNotFound() {
        Poll poll = poll(33L, "¿Otra?", "A", "B");
        PrivateGroup other = new PrivateGroup();
        other.setId(5L);
        poll.setGroup(other);
        when(pollRepository.findById(33L)).thenReturn(Optional.of(poll));

        assertThatThrownBy(() -> messageService.votePoll(1L, 33L, 1L, "alice"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void markChatSeen_createsVisit() {
        when(visitRepository.findByUserIdAndGroupId(1L, 1L)).thenReturn(Optional.empty());

        messageService.markChatSeen(1L, "alice");

        ArgumentCaptor<GroupVisit> captor = ArgumentCaptor.forClass(GroupVisit.class);
        verify(visitRepository).save(captor.capture());
        assertThat(captor.getValue().getChatSeenAt()).isNotNull();
    }

    @Test
    void markChatSeen_missingGroup_throwsNotFound() {
        when(groupRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> messageService.markChatSeen(1L, "alice")).isInstanceOf(ResourceNotFoundException.class);
    }

    private static Poll poll(Long id, String question, String... options) {
        Poll poll = new Poll();
        poll.setId(id);
        poll.setQuestion(question);
        long optionId = 1;
        for (String text : options) {
            PollOption option = new PollOption();
            option.setId(optionId++);
            option.setPoll(poll);
            option.setText(text);
            poll.getOptions().add(option);
        }
        return poll;
    }

    private static MessageReaction reaction(Long id, GroupMessage message, User user, String emoji) {
        MessageReaction reaction = new MessageReaction();
        reaction.setId(id);
        reaction.setMessage(message);
        reaction.setUser(user);
        reaction.setEmoji(emoji);
        return reaction;
    }

    @Test
    void getMessages_around_returnsBothSides() {
        GroupMessage before = message(30, bob);
        GroupMessage target = message(31, alice);
        GroupMessage after = message(32, bob);
        when(messageRepository.findTop25ByChannelIdAndThreadRootIsNullAndIdLessThanEqualOrderByIdDesc(4L, 31L))
                .thenReturn(List.of(target, before));
        when(messageRepository.findTop25ByChannelIdAndThreadRootIsNullAndIdGreaterThanOrderByIdAsc(4L, 31L))
                .thenReturn(List.of(after));

        assertThat(messageService.getMessages(1L, "alice", 4L, null, null, 31L))
                .extracting(GroupMessageResponseDTO::getId).containsExactly(30L, 31L, 32L);
    }

    @Test
    void sendMessage_respectsReadOnlySlowModeAndBannedWords() {
        general.setReadOnly(true);
        assertThatThrownBy(() -> messageService.sendMessage(1L, text("Hola"), "alice")).isInstanceOf(ForbiddenException.class);
        assertThat(messageService.sendMessage(1L, text("Aviso"), "bob").getContent()).isEqualTo("Aviso");

        general.setReadOnly(false);
        general.setSlowModeSeconds(30);
        GroupMessage last = message(40, alice);
        when(messageRepository.findTopByChannelIdAndAuthorIdOrderByIdDesc(4L, 1L)).thenReturn(Optional.of(last));
        assertThatThrownBy(() -> messageService.sendMessage(1L, text("Otra vez"), "alice"))
                .isInstanceOf(com.eduquest.api.exception.InvalidOperationException.class);
        last.setCreatedAt(LocalDateTime.now().minusMinutes(5));
        assertThat(messageService.sendMessage(1L, text("Ya puedo"), "alice").getContent()).isEqualTo("Ya puedo");

        group.setBannedWords("tonto\nspam");
        assertThatThrownBy(() -> messageService.sendMessage(1L, text("Eres un TONTO!"), "alice"))
                .isInstanceOf(BadRequestException.class);
        assertThat(messageService.sendMessage(1L, text("Tontería de examen"), "alice").getContent()).contains("Tontería");
    }

    @Test
    void sendMessage_notifiesReplyAndThreadAuthors() {
        GroupMessage original = message(5, bob);
        GroupMessageRequestDTO reply = text("Te respondo");
        reply.setReplyToId(5L);
        messageService.sendMessage(1L, reply, "alice");
        verify(notificationService).notifyReply(eq(bob), eq(alice), eq(group), eq(general), eq(50L), anyString());

        GroupMessageRequestDTO mention = text("@bob mira esto");
        mention.setThreadRootId(5L);
        messageService.sendMessage(1L, mention, "alice");
        verify(notificationService, times(1)).notifyReply(eq(bob), any(), any(), any(), any(), anyString());
        original.setDeleted(true);
    }

    @Test
    void savedMessages_toggleListAndLimit() {
        GroupMessage target = message(60, bob);
        target.getGroup().setName("Cálculo");
        SavedMessage entry = new SavedMessage();
        entry.setMessage(target);
        entry.setSavedAt(LocalDateTime.now());
        when(savedMessageRepository.findSavedIds(eq(1L), anyList())).thenReturn(List.of(60L));

        assertThat(messageService.setSaved(1L, 60L, true, "alice").isSaved()).isTrue();
        verify(savedMessageRepository).save(any(SavedMessage.class));

        when(savedMessageRepository.findByUserIdAndMessageId(1L, 60L)).thenReturn(Optional.of(entry));
        messageService.setSaved(1L, 60L, false, "alice");
        verify(savedMessageRepository).delete(entry);

        when(savedMessageRepository.findByUserIdOrderBySavedAtDesc(1L)).thenReturn(List.of(entry));
        assertThat(messageService.getSavedMessages("alice")).singleElement()
                .satisfies(saved -> assertThat(saved.channelName()).isEqualTo("general"));

        GroupMessage other = message(61, bob);
        when(savedMessageRepository.countByUserId(1L)).thenReturn((long) GroupMessageServiceImpl.MAX_SAVED);
        assertThatThrownBy(() -> messageService.setSaved(1L, 61L, true, "alice"))
                .isInstanceOf(com.eduquest.api.exception.InvalidOperationException.class);
        other.setDeleted(true);
        assertThatThrownBy(() -> messageService.setSaved(1L, 61L, true, "alice")).isInstanceOf(BadRequestException.class);
    }

    @Test
    void summarize_usesAiOrFallback() {
        GroupMessage first = message(70, bob);
        GroupMessage second = message(71, alice);
        GroupMessage third = message(72, bob);
        when(messageRepository.findForSummary(eq(4L), eq(0L), any(Pageable.class))).thenReturn(List.of(first, second, third));
        when(geminiClient.generate(anyString(), anyList(), anyInt(), anyBoolean()))
                .thenReturn(Optional.of("Resumen del canal"), Optional.empty());

        ChannelSummaryDTO ai = messageService.summarize(1L, null, "alice");
        assertThat(ai.generatedByAi()).isTrue();
        assertThat(ai.messageCount()).isEqualTo(3);

        ChannelSummaryDTO fallback = messageService.summarize(1L, null, "alice");
        assertThat(fallback.generatedByAi()).isFalse();
        assertThat(fallback.summary()).contains("@bob: 2 mensajes", "@alice: 1 mensaje");

        when(messageRepository.findForSummary(eq(4L), eq(0L), any(Pageable.class))).thenReturn(List.of());
        assertThat(messageService.summarize(1L, null, "alice").messageCount()).isZero();
    }
}
