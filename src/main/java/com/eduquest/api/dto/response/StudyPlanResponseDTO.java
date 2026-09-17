package com.eduquest.api.dto.response;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class StudyPlanResponseDTO {
    private Long id;
    private String courseName;
    private String difficulty;
    private String generatedContent;
    private LocalDateTime createdAt;
}