package com.eduquest.api.dto.response;

import java.time.LocalDateTime;

public record RoomMessageResponseDTO(Long id, String content, String imageUrl, String fileUrl, String fileName, String sticker,
                                     Long authorId, String authorUsername, String authorAvatarUrl, boolean mine,
                                     LocalDateTime createdAt, LocalDateTime editedAt, boolean deleted, MessageReplyDTO replyTo) {
}
