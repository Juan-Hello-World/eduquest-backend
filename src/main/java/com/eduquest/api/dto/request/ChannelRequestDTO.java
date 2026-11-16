package com.eduquest.api.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ChannelRequestDTO {
    @NotBlank(message = "Ponle un nombre al canal")
    @Size(min = 2, max = 40, message = "El nombre debe tener entre 2 y 40 caracteres")
    private String name;

    @Size(max = 200, message = "El tema no puede superar los 200 caracteres")
    private String topic;

    private Boolean readOnly;

    @Min(value = 0, message = "El modo lento no puede ser negativo")
    @Max(value = 3600, message = "El modo lento puede ser de hasta 1 hora")
    private Integer slowModeSeconds;
}
