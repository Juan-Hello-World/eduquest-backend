package com.eduquest.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ForumAnswerRequestDTO {
    @NotBlank(message = "Escribe tu respuesta")
    @Size(min = 2, max = 5000, message = "La respuesta debe tener entre 2 y 5000 caracteres")
    private String body;
}
