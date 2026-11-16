package com.eduquest.api.controller;

import com.eduquest.api.dto.request.ChannelRequestDTO;
import com.eduquest.api.dto.request.PollRequestDTO;
import com.eduquest.api.dto.request.PollVoteRequestDTO;
import com.eduquest.api.dto.request.ReactionRequestDTO;
import com.eduquest.api.dto.request.StickerRequestDTO;
import com.eduquest.api.dto.response.ChannelResponseDTO;
import com.eduquest.api.dto.response.GroupMessageResponseDTO;
import com.eduquest.api.dto.response.StickerResponseDTO;
import com.eduquest.api.dto.response.ThreadResponseDTO;
import com.eduquest.api.service.GroupCommunityService;
import com.eduquest.api.service.GroupMessageService;
import com.eduquest.api.service.StickerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
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
class GroupCommunityControllersTest {

    @Mock GroupCommunityService communityService;
    @Mock GroupMessageService messageService;
    @Mock StickerService stickerService;

    private Authentication authentication;

    @BeforeEach
    void setUp() {
        authentication = mock(Authentication.class);
        when(authentication.getName()).thenReturn("alice");
    }

    @Test
    void communityController_delegatesToService() {
        GroupCommunityController controller = new GroupCommunityController(communityService);
        ChannelResponseDTO channel = new ChannelResponseDTO(4L, "general", null, 0, false, 0, 0, 0);
        ChannelRequestDTO request = new ChannelRequestDTO();
        when(communityService.getChannels(1L, "alice")).thenReturn(List.of(channel));
        when(communityService.createChannel(1L, request, "alice")).thenReturn(channel);
        when(communityService.updateChannel(1L, 4L, request, "alice")).thenReturn(channel);
        when(communityService.getVoiceRoomUrl(1L, "alice")).thenReturn("https://meet.jit.si/EduQuest-x");
        when(communityService.getOnlineMembers(1L, "alice")).thenReturn(List.of(2L));

        assertThat(controller.getChannels(1L, authentication).getBody()).containsExactly(channel);
        assertThat(controller.createChannel(1L, request, authentication).getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(controller.updateChannel(1L, 4L, request, authentication).getBody()).isEqualTo(channel);
        assertThat(controller.deleteChannel(1L, 4L, authentication).getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(controller.setModerator(1L, 2L, true, authentication).getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(controller.setMuted(1L, true, authentication).getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(controller.getVoiceRoom(1L, authentication).getBody()).containsEntry("url", "https://meet.jit.si/EduQuest-x");
        assertThat(controller.getOnline(1L, authentication).getBody()).containsEntry("userIds", List.of(2L));
        verify(communityService).deleteChannel(1L, 4L, "alice");
        verify(communityService).setModerator(1L, 2L, true, "alice");
        verify(communityService).setMuted(1L, true, "alice");
    }

    @Test
    void messageController_newEndpointsDelegate() {
        GroupMessageController controller = new GroupMessageController(messageService);
        GroupMessageResponseDTO message = new GroupMessageResponseDTO();
        ReactionRequestDTO reaction = new ReactionRequestDTO();
        reaction.setEmoji("👍");
        when(messageService.search(1L, 4L, "hola", "alice")).thenReturn(List.of(message));
        when(messageService.getPinned(1L, 4L, "alice")).thenReturn(List.of(message));
        when(messageService.getThread(1L, 9L, "alice")).thenReturn(new ThreadResponseDTO(message, List.of()));
        when(messageService.toggleReaction(1L, 9L, "👍", "alice")).thenReturn(message);
        when(messageService.setPinned(1L, 9L, true, "alice")).thenReturn(message);

        assertThat(controller.search(1L, "hola", 4L, authentication).getBody()).hasSize(1);
        assertThat(controller.getPinned(1L, 4L, authentication).getBody()).hasSize(1);
        assertThat(controller.getThread(1L, 9L, authentication).getBody().root()).isSameAs(message);
        assertThat(controller.toggleReaction(1L, 9L, reaction, authentication).getBody()).isSameAs(message);
        assertThat(controller.setPinned(1L, 9L, true, authentication).getBody()).isSameAs(message);
    }

    @Test
    void pollController_createsAndVotes() {
        GroupPollController controller = new GroupPollController(messageService);
        GroupMessageResponseDTO message = new GroupMessageResponseDTO();
        PollVoteRequestDTO vote = new PollVoteRequestDTO();
        vote.setOptionId(3L);
        when(messageService.createPoll(eq(1L), any(PollRequestDTO.class), eq("alice"))).thenReturn(message);
        when(messageService.votePoll(1L, 7L, 3L, "alice")).thenReturn(message);

        assertThat(controller.createPoll(1L, new PollRequestDTO(), authentication).getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(controller.vote(1L, 7L, vote, authentication).getBody()).isSameAs(message);
    }

    @Test
    void stickerController_delegatesToService() {
        StickerController controller = new StickerController(stickerService);
        StickerResponseDTO sticker = new StickerResponseDTO(5L, "http://x/api/v1/files/a/b.png");
        StickerRequestDTO request = new StickerRequestDTO();
        request.setImageUrl(sticker.imageUrl());
        when(stickerService.getMyStickers("alice")).thenReturn(List.of(sticker));
        when(stickerService.addSticker(sticker.imageUrl(), "alice")).thenReturn(sticker);

        assertThat(controller.getMyStickers(authentication).getBody()).containsExactly(sticker);
        assertThat(controller.addSticker(request, authentication).getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(controller.deleteSticker(5L, authentication).getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(stickerService).deleteSticker(5L, "alice");
    }
}
