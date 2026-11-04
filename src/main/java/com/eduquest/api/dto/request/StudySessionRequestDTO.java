package com.eduquest.api.dto.request;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class StudySessionRequestDTO {
    @NotBlank (message = "El título de la sesión es obligtorio")
    @Size(max = 150)
    private String title;

    private String description;

    @NotNull (message = "La fecha programada es obligatoria")
    @Future(message = "La sesión debe programarse en una fecha futura")
    private LocalDateTime scheduledAt;

    @Pattern(regexp = "^https?://.+", message = "El enlace de la reunión debe comenzar con http:// o https://")
    private String meetingUrl;


}