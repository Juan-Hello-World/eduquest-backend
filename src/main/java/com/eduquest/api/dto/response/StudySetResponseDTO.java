package com.eduquest.api.dto.response;

import java.time.LocalDateTime;
import java.util.List;

public record StudySetResponseDTO(Long documentId, String type, List<Question> questions, List<Card> cards,
                                  boolean generatedByAi, LocalDateTime createdAt) {

    public record Question(String question, List<String> options, int answerIndex, String explanation) {
    }

    public record Card(String front, String back) {
    }
}
