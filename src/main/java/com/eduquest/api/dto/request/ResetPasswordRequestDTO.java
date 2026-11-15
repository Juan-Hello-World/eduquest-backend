package com.eduquest.api.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ResetPasswordRequestDTO {
    @NotBlank(message = "Escribe el correo de tu cuenta")
    @Email(message = "Escribe un correo válido")
    private String email;

    @NotBlank(message = "Escribe el código que te enviamos")
    @Pattern(regexp = "^\\d{6}$", message = "El código tiene 6 dígitos")
    private String code;

    @NotBlank(message = "Escribe tu nueva contraseña")
    @Size(min = 8, message = "La contraseña debe tener al menos 8 caracteres")
    @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$", message = "La contraseña debe tener al menos una letra y un número")
    private String password;
}
