package com.eduquest.api.dto.response;

import java.time.LocalDateTime;
import java.util.List;

public record RoomResponseDTO(Long id, String name, List<UserSummaryDTO> members, Long createdById, String lastMessage,
                              LocalDateTime lastActivityAt, long unreadCount) {
}
