package com.eduquest.api.dto.response;

import java.time.LocalDateTime;

public record DirectMessageResponseDTO(Long id, String content, String imageUrl, String fileUrl, String fileName,
                                       String sticker, Long senderId, boolean mine, boolean read,
                                       LocalDateTime createdAt, LocalDateTime editedAt, boolean deleted,
                                       MessageReplyDTO replyTo) {
}
