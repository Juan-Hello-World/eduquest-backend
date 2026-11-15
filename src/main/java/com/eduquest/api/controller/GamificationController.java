package com.eduquest.api.controller;

import com.eduquest.api.dto.response.GamificationProfileDTO;
import com.eduquest.api.dto.response.LeaderboardEntryDTO;
import com.eduquest.api.service.GamificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/gamification")
@Tag(name = "Logros", description = "Puntos, insignias y ranking de la comunidad.")
public class GamificationController {

    private final GamificationService gamificationService;

    public GamificationController(GamificationService gamificationService) {
        this.gamificationService = gamificationService;
    }

    @GetMapping("/me")
    @Operation(summary = "Mis logros", description = "Puntos totales, puntos de la semana, puesto en el ranking e insignias.")
    public ResponseEntity<GamificationProfileDTO> getMyProfile(Authentication authentication) {
        return ResponseEntity.ok(gamificationService.getProfile(authentication.getName()));
    }

    @GetMapping("/leaderboard")
    @Operation(summary = "Ranking", description = "Top 10. period: week (últimos 7 días, por defecto) o all.")
    public ResponseEntity<List<LeaderboardEntryDTO>> getLeaderboard(
            @RequestParam(defaultValue = "week") String period) {
        return ResponseEntity.ok(gamificationService.getLeaderboard(period));
    }
}
