package com.eduquest.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GroupPasswordRequestDTO {
    @NotBlank(message = "Escribe la nueva contraseña del grupo")
    @Size(min = 4, max = 50, message = "La contraseña del grupo debe tener entre 4 y 50 caracteres")
    private String password;
}
