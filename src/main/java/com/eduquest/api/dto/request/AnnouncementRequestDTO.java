package com.eduquest.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AnnouncementRequestDTO {
    @NotBlank(message = "Escribe un título para el aviso")
    @Size(min = 3, max = 100, message = "El título debe tener entre 3 y 100 caracteres")
    private String title;

    @NotBlank(message = "Escribe el mensaje del aviso")
    @Size(max = 1000, message = "El mensaje no puede superar los 1000 caracteres")
    private String message;

    @Size(max = 50)
    private String username;
}
