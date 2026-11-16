package com.eduquest.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PushTokenRequestDTO {
    @NotBlank(message = "Falta el token del dispositivo")
    @Pattern(regexp = "^(ExponentPushToken|ExpoPushToken)\\[[A-Za-z0-9_-]{10,100}]$", message = "Token de Expo no válido")
    private String token;
}
