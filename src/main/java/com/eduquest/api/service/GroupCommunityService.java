package com.eduquest.api.service;

import com.eduquest.api.dto.request.ChannelRequestDTO;
import com.eduquest.api.dto.response.ChannelResponseDTO;
import com.eduquest.api.dto.response.ModerationResponseDTO;
import com.eduquest.api.dto.response.VoiceRoomDTO;
import com.eduquest.api.entity.GroupChannel;
import com.eduquest.api.entity.PrivateGroup;

import java.util.List;

public interface GroupCommunityService {
    List<ChannelResponseDTO> getChannels(Long groupId, String username);
    ChannelResponseDTO createChannel(Long groupId, ChannelRequestDTO request, String username);
    ChannelResponseDTO updateChannel(Long groupId, Long channelId, ChannelRequestDTO request, String username);
    void deleteChannel(Long groupId, Long channelId, String username);
    void setModerator(Long groupId, Long memberId, boolean moderator, String username);
    void setMuted(Long groupId, boolean muted, String username);
    String getVoiceRoomUrl(Long groupId, String username);
    List<Long> getOnlineMembers(Long groupId, String username);
    GroupChannel resolveChannel(PrivateGroup group, Long channelId);
    void markChannelRead(Long groupId, Long channelId, String username);
    ModerationResponseDTO getModeration(Long groupId, String username);
    ModerationResponseDTO updateModeration(Long groupId, List<String> bannedWords, String username);
    VoiceRoomDTO getVoice(Long groupId, String username);
    VoiceRoomDTO setInVoice(Long groupId, boolean inVoice, String username);
}
