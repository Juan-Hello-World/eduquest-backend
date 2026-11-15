package com.eduquest.api.dto.response;

public record LeaderboardEntryDTO(int position, Long userId, String username, String avatarUrl,
                                  boolean advisor, long points) {
}
