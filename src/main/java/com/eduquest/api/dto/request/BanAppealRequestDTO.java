package com.eduquest.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BanAppealRequestDTO {
    @NotBlank(message = "Ingresa tu nombre de usuario")
    private String username;

    @NotBlank(message = "Ingresa tu contraseña")
    private String password;

    @NotBlank(message = "Cuéntanos por qué deberíamos revisar tu caso")
    @Size(min = 20, max = 1000, message = "La apelación debe tener entre 20 y 1000 caracteres")
    private String message;
}
