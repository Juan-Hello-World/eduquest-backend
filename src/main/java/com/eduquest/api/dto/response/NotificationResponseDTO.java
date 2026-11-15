package com.eduquest.api.dto.response;

import java.time.LocalDateTime;

public record NotificationResponseDTO(Long id, String type, String title, String body, String link,
                                      boolean read, LocalDateTime createdAt) {
}
