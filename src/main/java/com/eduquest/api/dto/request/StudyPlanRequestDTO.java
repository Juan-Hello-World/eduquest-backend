package com.eduquest.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class StudyPlanRequestDTO {
    @NotBlank
    private String courseName;

    @NotBlank
    private String difficulty; // Baja, Media, Alta
}