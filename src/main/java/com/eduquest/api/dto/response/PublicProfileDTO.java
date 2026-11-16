package com.eduquest.api.dto.response;

import java.time.LocalDateTime;
import java.util.List;

public record PublicProfileDTO(Long id,
                               String username,
                               String avatarUrl,
                               boolean advisor,
                               LocalDateTime memberSince,
                               long points,
                               long weeklyPoints,
                               Integer rank,
                               GamificationStatsDTO stats,
                               List<BadgeDTO> badges,
                               long followers,
                               long following,
                               boolean followedByMe,
                               boolean me) {
}
