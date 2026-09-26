package com.eduquest.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class StudyPlanRequestDTO {
    @NotBlank
    private String courseName;

    @NotBlank
    private String difficulty; // Baja, Media, Alta
}