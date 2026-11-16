package com.eduquest.api.dto.response;

public record ChannelResponseDTO(Long id, String name, String topic, int position, boolean readOnly,
                                 int slowModeSeconds, long unreadCount, long mentionCount) {
}
