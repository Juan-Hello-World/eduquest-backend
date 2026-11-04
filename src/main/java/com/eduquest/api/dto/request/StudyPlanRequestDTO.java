package com.eduquest.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class StudyPlanRequestDTO {
    @NotBlank (message="El nombre del curso o tema es obligatorio")
    @Size(max = 100, message = "El tema no puede superar los 100 caracteres")
    private String courseName;

    @NotBlank (message = "La dificutad percibida es obligatoria (Baja, Media, Alta)")
    @Pattern(regexp = "^(?i)(Baja|Media|Alta)$", message = "La dificultad debe ser Baja, Media o Alta")
    private String difficulty; // Baja, Media, Alta
}