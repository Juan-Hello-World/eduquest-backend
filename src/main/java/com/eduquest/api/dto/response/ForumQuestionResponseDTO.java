package com.eduquest.api.dto.response;

import java.time.LocalDateTime;

public record ForumQuestionResponseDTO(Long id,
                                       String title,
                                       String body,
                                       String course,
                                       UserSummaryDTO author,
                                       LocalDateTime createdAt,
                                       long answerCount,
                                       boolean solved,
                                       boolean mine,
                                       boolean canDelete) {
}
