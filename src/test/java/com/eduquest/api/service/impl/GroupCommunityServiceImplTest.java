package com.eduquest.api.service.impl;

import com.eduquest.api.dto.request.ChannelRequestDTO;
import com.eduquest.api.dto.response.ChannelResponseDTO;
import com.eduquest.api.entity.GroupChannel;
import com.eduquest.api.entity.GroupVisit;
import com.eduquest.api.entity.PrivateGroup;
import com.eduquest.api.entity.Role;
import com.eduquest.api.entity.RoleName;
import com.eduquest.api.entity.User;
import com.eduquest.api.exception.BadRequestException;
import com.eduquest.api.exception.DuplicateResourceException;
import com.eduquest.api.exception.ForbiddenException;
import com.eduquest.api.exception.InvalidOperationException;
import com.eduquest.api.exception.ResourceNotFoundException;
import com.eduquest.api.realtime.PresenceService;
import com.eduquest.api.realtime.RealtimeEvent;
import com.eduquest.api.repository.GroupChannelRepository;
import com.eduquest.api.repository.GroupMessageRepository;
import com.eduquest.api.repository.GroupVisitRepository;
import com.eduquest.api.repository.MessageReactionRepository;
import com.eduquest.api.repository.PollRepository;
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
import org.springframework.context.ApplicationEventPublisher;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class GroupCommunityServiceImplTest {

    @Mock PrivateGroupRepository groupRepository;
    @Mock GroupChannelRepository channelRepository;
    @Mock GroupMessageRepository messageRepository;
    @Mock MessageReactionRepository reactionRepository;
    @Mock PollRepository pollRepository;
    @Mock GroupVisitRepository visitRepository;
    @Mock UserRepository userRepository;
    @Mock PresenceService presenceService;
    @Mock ApplicationEventPublisher eventPublisher;
    @Mock com.eduquest.api.repository.ChannelReadRepository channelReadRepository;

    @InjectMocks GroupCommunityServiceImpl communityService;

    private User owner;
    private User member;
    private PrivateGroup group;
    private final List<GroupChannel> channels = new ArrayList<>();

    @BeforeEach
    void setUp() {
        owner = user(1L, "owner");
        member = user(2L, "member");
        group = new PrivateGroup();
        group.setId(10L);
        group.setName("Física Ñandú");
        group.setCreator(owner);
        group.getMembers().add(owner);
        group.getMembers().add(member);
        when(userRepository.findByUsername("owner")).thenReturn(Optional.of(owner));
        when(userRepository.findByUsername("member")).thenReturn(Optional.of(member));
        when(groupRepository.findById(10L)).thenReturn(Optional.of(group));
        when(groupRepository.existsByIdAndMembersId(eq(10L), any())).thenReturn(true);
        when(channelRepository.findByGroupIdOrderByPositionAscIdAsc(10L)).thenAnswer(invocation -> List.copyOf(channels));
        when(channelRepository.countByGroupId(10L)).thenAnswer(invocation -> (long) channels.size());
        when(channelRepository.save(any(GroupChannel.class))).thenAnswer(invocation -> {
            GroupChannel channel = invocation.getArgument(0);
            if (channel.getId() == null) {
                channel.setId(100L + channels.size());
                channels.add(channel);
            }
            return channel;
        });
    }

    private static User user(Long id, String username) {
        User user = new User();
        user.setId(id);
        user.setUsername(username);
        return user;
    }

    private static ChannelRequestDTO request(String name, String topic) {
        ChannelRequestDTO request = new ChannelRequestDTO();
        request.setName(name);
        request.setTopic(topic);
        return request;
    }

    @Test
    void getChannels_createsGeneralAndBackfillsMessages() {
        List<ChannelResponseDTO> result = communityService.getChannels(10L, "member");

        assertThat(result).extracting(ChannelResponseDTO::name).containsExactly("general");
        verify(messageRepository).assignChannel(eq(10L), any(GroupChannel.class));
        communityService.getChannels(10L, "member");
        verify(channelRepository, org.mockito.Mockito.times(1)).save(any(GroupChannel.class));
    }

    @Test
    void getChannels_notMember_isForbidden() {
        when(groupRepository.existsByIdAndMembersId(10L, 2L)).thenReturn(false);

        assertThatThrownBy(() -> communityService.getChannels(10L, "member")).isInstanceOf(ForbiddenException.class);
    }

    @Test
    void createChannel_normalizesNameAndBroadcasts() {
        ChannelResponseDTO created = communityService.createChannel(10L, request("  Dudas de Ñoño Álgebra!! ", " "), "owner");

        assertThat(created.name()).isEqualTo("dudas-de-nono-algebra");
        assertThat(created.topic()).isNull();
        assertThat(created.position()).isEqualTo(1);
        ArgumentCaptor<RealtimeEvent> captor = ArgumentCaptor.forClass(RealtimeEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue().getPayload()).containsEntry("type", "group.channels");
        assertThat(captor.getValue().getUserIds()).containsExactlyInAnyOrder(1L, 2L);
    }

    @Test
    void createChannel_rejectsDuplicatesInvalidNamesAndNonManagers() {
        communityService.createChannel(10L, request("tareas", null), "owner");

        assertThatThrownBy(() -> communityService.createChannel(10L, request("Tareas", null), "owner"))
                .isInstanceOf(DuplicateResourceException.class);
        assertThatThrownBy(() -> communityService.createChannel(10L, request("!!", null), "owner"))
                .isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> communityService.createChannel(10L, request("otro", null), "member"))
                .isInstanceOf(ForbiddenException.class);

        group.getModerators().add(member);
        assertThat(communityService.createChannel(10L, request("otro", "Tema"), "member").topic()).isEqualTo("Tema");
    }

    @Test
    void createChannel_respectsTheLimit() {
        for (int index = 0; index < GroupCommunityServiceImpl.MAX_CHANNELS; index++) {
            GroupChannel channel = new GroupChannel();
            channel.setId((long) index);
            channel.setName("canal-" + index);
            channel.setPosition(index);
            channels.add(channel);
        }

        assertThatThrownBy(() -> communityService.createChannel(10L, request("extra", null), "owner"))
                .isInstanceOf(InvalidOperationException.class);
    }

    @Test
    void updateChannel_renamesAndValidatesOwnership() {
        communityService.getChannels(10L, "owner");
        GroupChannel general = channels.getFirst();
        general.setGroup(group);
        when(channelRepository.findById(general.getId())).thenReturn(Optional.of(general));

        ChannelResponseDTO updated = communityService.updateChannel(10L, general.getId(), request("Anuncios", "Avisos"), "owner");

        assertThat(updated.name()).isEqualTo("anuncios");
        assertThat(updated.topic()).isEqualTo("Avisos");
        assertThatThrownBy(() -> communityService.updateChannel(10L, 999L, request("x1", null), "owner"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void deleteChannel_keepsAtLeastOneAndCleansContent() {
        communityService.getChannels(10L, "owner");
        communityService.createChannel(10L, request("extra", null), "owner");
        GroupChannel general = channels.get(0);
        GroupChannel extra = channels.get(1);
        general.setGroup(group);
        extra.setGroup(group);
        when(channelRepository.findById(general.getId())).thenReturn(Optional.of(general));
        when(channelRepository.findById(extra.getId())).thenReturn(Optional.of(extra));
        when(messageRepository.findPollIdsByChannelId(extra.getId())).thenReturn(List.of(7L));
        org.mockito.Mockito.doAnswer(invocation -> channels.remove(extra)).when(channelRepository).delete(extra);

        communityService.deleteChannel(10L, extra.getId(), "owner");

        verify(reactionRepository).deleteByChannelId(extra.getId());
        verify(messageRepository).deleteByChannelId(extra.getId());
        verify(pollRepository).deleteByIds(List.of(7L));
        assertThatThrownBy(() -> communityService.deleteChannel(10L, general.getId(), "owner"))
                .isInstanceOf(InvalidOperationException.class);
    }

    @Test
    void deleteChannel_withoutPolls_skipsPollCleanup() {
        communityService.getChannels(10L, "owner");
        communityService.createChannel(10L, request("extra", null), "owner");
        GroupChannel extra = channels.get(1);
        extra.setGroup(group);
        when(channelRepository.findById(extra.getId())).thenReturn(Optional.of(extra));

        communityService.deleteChannel(10L, extra.getId(), "owner");

        verify(pollRepository, never()).deleteByIds(anyList());
    }

    @Test
    void setModerator_onlyCreatorCanPromoteMembers() {
        communityService.setModerator(10L, 2L, true, "owner");
        assertThat(group.isModerator(member)).isTrue();

        assertThatThrownBy(() -> communityService.setModerator(10L, 1L, true, "member"))
                .isInstanceOf(ForbiddenException.class);
        assertThatThrownBy(() -> communityService.setModerator(10L, 1L, true, "owner"))
                .isInstanceOf(InvalidOperationException.class);
        assertThatThrownBy(() -> communityService.setModerator(10L, 77L, true, "owner"))
                .isInstanceOf(ResourceNotFoundException.class);

        communityService.setModerator(10L, 2L, false, "owner");
        assertThat(group.isModerator(member)).isFalse();
    }

    @Test
    void setModerator_platformAdminCanAlsoManage() {
        member.getRoles().add(new Role(3L, RoleName.ROLE_ADMIN));
        User third = user(3L, "third");
        group.getMembers().add(third);

        communityService.setModerator(10L, 3L, true, "member");

        assertThat(group.isModerator(third)).isTrue();
    }

    @Test
    void setMuted_createsOrUpdatesTheVisit() {
        when(visitRepository.findByUserIdAndGroupId(2L, 10L)).thenReturn(Optional.empty());

        communityService.setMuted(10L, true, "member");

        ArgumentCaptor<GroupVisit> captor = ArgumentCaptor.forClass(GroupVisit.class);
        verify(visitRepository).save(captor.capture());
        assertThat(captor.getValue().isMuted()).isTrue();
    }

    @Test
    void getVoiceRoomUrl_isCreatedOnceAndReused() {
        String first = communityService.getVoiceRoomUrl(10L, "member");
        String second = communityService.getVoiceRoomUrl(10L, "owner");

        assertThat(first).startsWith("https://meet.jit.si/EduQuest-fisica-nandu-").isEqualTo(second);
        verify(groupRepository, org.mockito.Mockito.times(1)).save(group);
    }

    @Test
    void getOnlineMembers_filtersByPresence() {
        when(presenceService.isOnline(2L)).thenReturn(true);

        assertThat(communityService.getOnlineMembers(10L, "owner")).containsExactly(2L);
    }

    @Test
    void resolveChannel_usesDefaultOrValidatesGroup() {
        GroupChannel resolved = communityService.resolveChannel(group, null);
        assertThat(resolved.getName()).isEqualTo("general");

        GroupChannel foreign = new GroupChannel();
        foreign.setId(500L);
        PrivateGroup other = new PrivateGroup();
        other.setId(11L);
        foreign.setGroup(other);
        when(channelRepository.findById(500L)).thenReturn(Optional.of(foreign));
        assertThatThrownBy(() -> communityService.resolveChannel(group, 500L)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void unknownGroupOrUser_isNotFound() {
        assertThatThrownBy(() -> communityService.getChannels(99L, "owner")).isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> communityService.getChannels(10L, "ghost")).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void channels_reportUnreadAndMentions() {
        communityService.getChannels(10L, "owner");
        GroupChannel general = channels.getFirst();
        List<Object[]> unread = new ArrayList<>();
        unread.add(new Object[]{general.getId(), 5L});
        List<Object[]> mentions = new ArrayList<>();
        mentions.add(new Object[]{general.getId(), 2L});
        when(messageRepository.countUnreadByChannel(10L, 2L)).thenReturn(unread);
        when(messageRepository.countMentionsByChannel(10L, 2L, "%@member%")).thenReturn(mentions);

        ChannelResponseDTO channel = communityService.getChannels(10L, "member").getFirst();

        assertThat(channel.unreadCount()).isEqualTo(5L);
        assertThat(channel.mentionCount()).isEqualTo(2L);
    }

    @Test
    void createChannel_appliesReadOnlyAndSlowMode() {
        ChannelRequestDTO request = request("anuncios", null);
        request.setReadOnly(true);
        request.setSlowModeSeconds(30);

        ChannelResponseDTO created = communityService.createChannel(10L, request, "owner");

        assertThat(created.readOnly()).isTrue();
        assertThat(created.slowModeSeconds()).isEqualTo(30);
    }

    @Test
    void markChannelRead_storesLastMessage() {
        communityService.getChannels(10L, "owner");
        GroupChannel general = channels.getFirst();
        general.setGroup(group);
        when(channelRepository.findById(general.getId())).thenReturn(Optional.of(general));
        com.eduquest.api.entity.GroupMessage last = new com.eduquest.api.entity.GroupMessage();
        last.setId(77L);
        when(messageRepository.findTopByChannelIdOrderByIdDesc(general.getId())).thenReturn(Optional.of(last));

        communityService.markChannelRead(10L, general.getId(), "member");

        ArgumentCaptor<com.eduquest.api.entity.ChannelRead> captor = ArgumentCaptor.forClass(com.eduquest.api.entity.ChannelRead.class);
        verify(channelReadRepository).save(captor.capture());
        assertThat(captor.getValue().getLastReadMessageId()).isEqualTo(77L);
    }

    @Test
    void moderation_normalizesWordsAndRequiresManager() {
        assertThat(communityService.updateModeration(10L, List.of(" Tonto ", "tonto", "Ñoño", " "), "owner").bannedWords())
                .containsExactly("tonto", "nono");
        assertThat(communityService.getModeration(10L, "member").bannedWords()).containsExactly("tonto", "nono");
        assertThatThrownBy(() -> communityService.updateModeration(10L, List.of("x"), "member"))
                .isInstanceOf(ForbiddenException.class);
        List<String> tooMany = java.util.stream.IntStream.range(0, 51).mapToObj(index -> "palabra" + index).toList();
        assertThatThrownBy(() -> communityService.updateModeration(10L, tooMany, "owner")).isInstanceOf(BadRequestException.class);
        communityService.updateModeration(10L, List.of(), "owner");
        assertThat(group.getBannedWords()).isNull();
    }

    @Test
    void voice_tracksParticipantsWhileOnline() {
        when(presenceService.isOnline(2L)).thenReturn(true);

        assertThat(communityService.setInVoice(10L, true, "member").participants())
                .extracting(item -> item.username()).containsExactly("member");
        assertThat(communityService.getVoice(10L, "owner").url()).startsWith("https://meet.jit.si/");

        when(presenceService.isOnline(2L)).thenReturn(false);
        assertThat(communityService.getVoice(10L, "owner").participants()).isEmpty();

        when(presenceService.isOnline(2L)).thenReturn(true);
        communityService.setInVoice(10L, true, "member");
        assertThat(communityService.setInVoice(10L, false, "member").participants()).isEmpty();
    }

    @Test
    void onlineMembers_hideInvisibleUsers() {
        when(presenceService.isOnline(org.mockito.ArgumentMatchers.anyLong())).thenReturn(true);
        member.setAvailability(com.eduquest.api.entity.Availability.INVISIBLE);

        assertThat(communityService.getOnlineMembers(10L, "owner")).containsExactly(1L);
    }
}
