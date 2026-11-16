package com.eduquest.api.dto.response;

import java.time.LocalDateTime;

public record SavedMessageDTO(GroupMessageResponseDTO message, Long groupId, String groupName, String channelName,
                              LocalDateTime savedAt) {
}
