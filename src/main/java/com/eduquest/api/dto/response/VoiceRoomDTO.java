package com.eduquest.api.dto.response;

import java.util.List;

public record VoiceRoomDTO(String url, List<UserSummaryDTO> participants) {
}
