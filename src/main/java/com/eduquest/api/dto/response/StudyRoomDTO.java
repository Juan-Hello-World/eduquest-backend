package com.eduquest.api.dto.response;

import java.time.LocalDateTime;
import java.util.List;

public record StudyRoomDTO(boolean active, int focusMinutes, int breakMinutes, LocalDateTime startedAt,
                           LocalDateTime serverTime, Long startedById, List<UserSummaryDTO> participants) {
}
