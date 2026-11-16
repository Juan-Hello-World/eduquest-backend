package com.eduquest.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class PollRequestDTO {
    private Long channelId;

    @NotBlank(message = "Escribe la pregunta de la encuesta")
    @Size(min = 3, max = 200, message = "La pregunta debe tener entre 3 y 200 caracteres")
    private String question;

    @NotNull(message = "Agrega las opciones")
    @Size(min = 2, max = 6, message = "La encuesta debe tener entre 2 y 6 opciones")
    private List<@NotBlank(message = "Ninguna opción puede quedar vacía") @Size(max = 100, message = "Cada opción puede tener hasta 100 caracteres") String> options;
}
