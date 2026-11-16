package com.eduquest.api.dto.response;

import java.time.LocalDateTime;

public record ForumAnswerResponseDTO(Long id,
                                     String body,
                                     UserSummaryDTO author,
                                     LocalDateTime createdAt,
                                     long score,
                                     int myVote,
                                     boolean accepted,
                                     boolean mine,
                                     boolean canDelete) {
}
