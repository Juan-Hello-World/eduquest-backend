package com.eduquest.api.service;

import com.eduquest.api.dto.request.GroupMessageRequestDTO;
import com.eduquest.api.dto.request.PollRequestDTO;
import com.eduquest.api.dto.response.ChannelSummaryDTO;
import com.eduquest.api.dto.response.GroupMessageResponseDTO;
import com.eduquest.api.dto.response.SavedMessageDTO;
import com.eduquest.api.dto.response.ThreadResponseDTO;

import java.util.List;

public interface GroupMessageService {
    List<GroupMessageResponseDTO> getMessages(Long groupId, String username, Long channelId, Long after, Long before, Long around);
    GroupMessageResponseDTO sendMessage(Long groupId, GroupMessageRequestDTO request, String username);
    GroupMessageResponseDTO editMessage(Long groupId, Long messageId, String content, String username);
    GroupMessageResponseDTO deleteMessage(Long groupId, Long messageId, String username);
    GroupMessageResponseDTO toggleReaction(Long groupId, Long messageId, String emoji, String username);
    GroupMessageResponseDTO setPinned(Long groupId, Long messageId, boolean pinned, String username);
    List<GroupMessageResponseDTO> getPinned(Long groupId, Long channelId, String username);
    ThreadResponseDTO getThread(Long groupId, Long messageId, String username);
    List<GroupMessageResponseDTO> search(Long groupId, Long channelId, String query, String username);
    GroupMessageResponseDTO createPoll(Long groupId, PollRequestDTO request, String username);
    GroupMessageResponseDTO votePoll(Long groupId, Long pollId, Long optionId, String username);
    void markChatSeen(Long groupId, String username);
    GroupMessageResponseDTO setSaved(Long groupId, Long messageId, boolean saved, String username);
    List<SavedMessageDTO> getSavedMessages(String username);
    ChannelSummaryDTO summarize(Long groupId, Long channelId, String username);
}
