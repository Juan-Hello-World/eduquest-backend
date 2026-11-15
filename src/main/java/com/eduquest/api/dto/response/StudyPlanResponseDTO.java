package com.eduquest.api.dto.response;

import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
public class StudyPlanResponseDTO {
    private Long id;
    private String courseName;
    private String difficulty;
    private String generatedContent;
    private LocalDateTime createdAt;
    private int totalSteps;
    private List<Integer> completedSteps;
}