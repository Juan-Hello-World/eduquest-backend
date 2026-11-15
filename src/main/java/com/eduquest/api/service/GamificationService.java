package com.eduquest.api.service;

import com.eduquest.api.dto.response.GamificationProfileDTO;
import com.eduquest.api.dto.response.LeaderboardEntryDTO;

import java.util.List;

public interface GamificationService {
    GamificationProfileDTO getProfile(String username);
    List<LeaderboardEntryDTO> getLeaderboard(String period);
    void syncBadges(Long userId);
}
