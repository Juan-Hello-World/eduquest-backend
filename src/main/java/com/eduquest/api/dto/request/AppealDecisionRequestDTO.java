package com.eduquest.api.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AppealDecisionRequestDTO {
    @NotNull(message = "Indica si aceptas o rechazas la apelación")
    private Boolean accepted;

    @Size(max = 500, message = "La respuesta no puede superar los 500 caracteres")
    private String response;
}
