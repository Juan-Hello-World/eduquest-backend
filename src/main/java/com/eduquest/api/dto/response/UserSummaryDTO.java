package com.eduquest.api.dto.response;

public record UserSummaryDTO(Long id, String username, String avatarUrl, boolean advisor) {
}
