package com.eduquest.api.dto.response;

public record UserStatusDTO(Long userId, String availability, String statusText, String statusEmoji, boolean online) {
}
