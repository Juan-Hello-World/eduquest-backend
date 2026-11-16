package com.eduquest.api.service;

import com.eduquest.api.dto.request.GroupMessageRequestDTO;
import com.eduquest.api.dto.response.ConversationResponseDTO;
import com.eduquest.api.dto.response.DirectMessageResponseDTO;
import com.eduquest.api.dto.response.UserSummaryDTO;

import java.util.List;

public interface DirectMessageService {
    List<ConversationResponseDTO> getConversations(String username);
    List<DirectMessageResponseDTO> getMessages(String username, Long otherUserId, Long after, Long before);
    DirectMessageResponseDTO sendMessage(String username, Long otherUserId, GroupMessageRequestDTO request);
    DirectMessageResponseDTO editMessage(String username, Long otherUserId, Long messageId, String content);
    DirectMessageResponseDTO deleteMessage(String username, Long otherUserId, Long messageId);
    void markRead(String username, Long otherUserId);
    long countUnread(String username);
    List<UserSummaryDTO> searchUsers(String username, String query);
    UserSummaryDTO getUserSummary(Long userId);
}
