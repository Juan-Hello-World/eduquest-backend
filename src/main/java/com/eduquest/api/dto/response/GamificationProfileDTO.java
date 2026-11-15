package com.eduquest.api.dto.response;

import java.util.List;

public record GamificationProfileDTO(long points, long weeklyPoints, Integer rank,
                                     GamificationStatsDTO stats, List<BadgeDTO> badges) {
}
