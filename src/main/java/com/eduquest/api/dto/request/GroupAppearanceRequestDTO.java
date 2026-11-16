package com.eduquest.api.dto.request;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GroupAppearanceRequestDTO {
    @Pattern(regexp = "^#[0-9a-fA-F]{6}$", message = "El color debe tener el formato #RRGGBB")
    private String color;

    @Size(max = 200, message = "La dirección de la foto es demasiado larga")
    private String iconUrl;

    @Size(max = 200, message = "La dirección de la portada es demasiado larga")
    private String bannerUrl;
}
