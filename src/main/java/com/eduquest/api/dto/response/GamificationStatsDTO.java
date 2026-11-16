package com.eduquest.api.dto.response;

public record GamificationStatsDTO(long documents, long reviews, long sessionsHosted, long confirmations,
                                   long attendances, long noShows, long sessionReviews) {
}
