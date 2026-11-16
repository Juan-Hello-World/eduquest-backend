package com.eduquest.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReactionRequestDTO {
    @NotBlank(message = "Elige un emoji")
    @Size(max = 16, message = "Reacción no válida")
    private String emoji;
}
