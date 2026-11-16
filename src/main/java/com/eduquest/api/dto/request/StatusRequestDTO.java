package com.eduquest.api.dto.request;

import com.eduquest.api.entity.Availability;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class StatusRequestDTO {
    @NotNull(message = "Elige tu disponibilidad")
    private Availability availability;

    @Size(max = 80, message = "El estado puede tener hasta 80 caracteres")
    private String statusText;

    @Size(max = 16, message = "Emoji no válido")
    private String statusEmoji;
}
