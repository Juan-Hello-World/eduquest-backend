package com.eduquest.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class WarningRequestDTO {
    @NotBlank(message = "Escribe el motivo de la advertencia")
    @Size(min = 10, max = 500, message = "La advertencia debe tener entre 10 y 500 caracteres")
    private String message;
}
