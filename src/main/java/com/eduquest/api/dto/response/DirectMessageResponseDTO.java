package com.eduquest.api.dto.response;

import java.time.LocalDateTime;

public record DirectMessageResponseDTO(Long id, String content, Long senderId, boolean mine,
                                       boolean read, LocalDateTime createdAt) {
}
