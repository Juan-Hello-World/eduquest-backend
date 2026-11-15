package com.eduquest.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class CourseInputDTO {
    @NotBlank(message = "El nombre del curso es obligatorio")
    @Size(max = 100, message = "El curso no puede superar los 100 caracteres")
    private String name;

    @NotBlank(message = "La dificultad es obligatoria")
    @Pattern(regexp = "^(?i)(Baja|Media|Alta)$", message = "La dificultad debe ser Baja, Media o Alta")
    private String difficulty;

    public CourseInputDTO(String name, String difficulty) {
        this.name = name;
        this.difficulty = difficulty;
    }
}
