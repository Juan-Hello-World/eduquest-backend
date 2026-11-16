package com.eduquest.api.dto.response;

import java.util.List;

public record ForumThreadResponseDTO(ForumQuestionResponseDTO question, List<ForumAnswerResponseDTO> answers,
                                     boolean canAccept) {
}
