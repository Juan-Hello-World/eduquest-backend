package com.eduquest.api.dto.response;

import java.time.LocalDateTime;

public record ConversationResponseDTO(UserSummaryDTO partner, String lastMessage, boolean lastMessageMine,
                                      LocalDateTime lastMessageAt, long unreadCount) {
}
