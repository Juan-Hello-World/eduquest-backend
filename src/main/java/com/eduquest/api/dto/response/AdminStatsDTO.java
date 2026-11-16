package com.eduquest.api.dto.response;

public record AdminStatsDTO(long users, long newUsersThisWeek, long suspendedUsers, long documents,
                            long documentsThisWeek, long groups, long sessionsThisWeek, long openReports) {
}
