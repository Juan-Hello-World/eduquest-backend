package com.eduquest.api.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ForgotPasswordRequestDTO {
    @NotBlank(message = "Escribe el correo de tu cuenta")
    @Email(message = "Escribe un correo válido")
    private String email;
}
