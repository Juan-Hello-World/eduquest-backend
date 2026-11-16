package com.eduquest.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class VerifyEmailRequestDTO {
    @NotBlank(message = "Ingresa el código")
    @Pattern(regexp = "^\\s*\\d{6}\\s*$", message = "El código tiene 6 dígitos")
    private String code;
}
