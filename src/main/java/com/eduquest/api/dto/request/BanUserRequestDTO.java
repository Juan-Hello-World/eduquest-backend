package com.eduquest.api.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BanUserRequestDTO {
    @NotBlank(message = "Indica qué norma incumplió el usuario")
    @Size(min = 10, max = 500, message = "El motivo debe tener entre 10 y 500 caracteres")
    private String reason;

    @Min(value = 1, message = "La suspensión debe durar al menos 1 día")
    @Max(value = 30, message = "La suspensión temporal dura como máximo 30 días")
    private Integer days;
}
