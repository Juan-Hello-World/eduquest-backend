package com.eduquest.api.dto.response;

import java.time.LocalDateTime;

public record GroupInviteResponseDTO(String code, String token, LocalDateTime expiresAt) {
}
