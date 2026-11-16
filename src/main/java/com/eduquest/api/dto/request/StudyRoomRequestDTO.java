package com.eduquest.api.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class StudyRoomRequestDTO {
    @Min(value = 5, message = "El bloque de estudio dura al menos 5 minutos")
    @Max(value = 90, message = "El bloque de estudio dura hasta 90 minutos")
    private int focusMinutes = 25;

    @Min(value = 1, message = "El descanso dura al menos 1 minuto")
    @Max(value = 30, message = "El descanso dura hasta 30 minutos")
    private int breakMinutes = 5;
}
