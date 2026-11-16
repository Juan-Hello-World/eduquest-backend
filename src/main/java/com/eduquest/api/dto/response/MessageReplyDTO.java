package com.eduquest.api.dto.response;

public record MessageReplyDTO(Long id, Long authorId, String authorUsername, String preview, boolean deleted) {
}
