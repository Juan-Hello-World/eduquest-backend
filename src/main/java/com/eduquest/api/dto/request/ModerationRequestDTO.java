package com.eduquest.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class ModerationRequestDTO {
    @NotNull(message = "Envía la lista de palabras")
    @Size(max = 50, message = "Puedes bloquear hasta 50 palabras")
    private List<@NotBlank @Size(max = 40, message = "Cada palabra puede tener hasta 40 caracteres") String> bannedWords;
}
