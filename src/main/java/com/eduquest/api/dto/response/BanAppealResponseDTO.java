package com.eduquest.api.dto.response;

import java.time.LocalDateTime;

public record BanAppealResponseDTO(Long id,
                                   Long userId,
                                   String username,
                                   String banReason,
                                   LocalDateTime bannedUntil,
                                   String message,
                                   String status,
                                   LocalDateTime createdAt,
                                   String reviewedBy,
                                   LocalDateTime reviewedAt,
                                   String response) {
}
