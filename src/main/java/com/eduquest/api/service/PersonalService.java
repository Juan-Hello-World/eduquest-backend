package com.eduquest.api.service;

import com.eduquest.api.dto.request.PinnedChatRequestDTO;
import com.eduquest.api.dto.request.StatusRequestDTO;
import com.eduquest.api.dto.response.PinnedChatDTO;
import com.eduquest.api.dto.response.UserStatusDTO;

import java.util.List;

public interface PersonalService {
    UserStatusDTO updateStatus(String username, StatusRequestDTO request);
    List<UserStatusDTO> getStatuses(List<Long> userIds);
    List<PinnedChatDTO> getPinnedChats(String username);
    List<PinnedChatDTO> setPinned(String username, PinnedChatRequestDTO request);
}
