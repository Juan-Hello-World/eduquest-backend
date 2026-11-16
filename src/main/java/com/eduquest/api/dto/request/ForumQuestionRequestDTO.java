package com.eduquest.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ForumQuestionRequestDTO {
    @NotBlank(message = "Escribe un título para tu pregunta")
    @Size(min = 8, max = 150, message = "El título debe tener entre 8 y 150 caracteres")
    private String title;

    @NotBlank(message = "Describe tu duda")
    @Size(min = 15, max = 5000, message = "La descripción debe tener entre 15 y 5000 caracteres")
    private String body;

    @Size(max = 100, message = "El curso no puede superar los 100 caracteres")
    private String course;
}
