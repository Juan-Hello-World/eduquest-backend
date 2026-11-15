package com.eduquest.api.service;

import com.eduquest.api.dto.request.GroupMessageRequestDTO;
import com.eduquest.api.dto.response.GroupMessageResponseDTO;

import java.util.List;

public interface GroupMessageService {
    List<GroupMessageResponseDTO> getMessages(Long groupId, String username, Long after, Long before);
    GroupMessageResponseDTO sendMessage(Long groupId, GroupMessageRequestDTO request, String username);
    void markChatSeen(Long groupId, String username);
}
