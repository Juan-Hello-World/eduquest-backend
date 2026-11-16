package com.eduquest.api.dto.response;

import java.util.List;

public record ModerationResponseDTO(List<String> bannedWords) {
}
