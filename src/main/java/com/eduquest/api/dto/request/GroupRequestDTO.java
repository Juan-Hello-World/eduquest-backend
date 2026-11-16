package com.eduquest.api.dto.request;

import com.eduquest.api.entity.GroupVisibility;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;


@Getter
@Setter
public class GroupRequestDTO {
    @NotBlank(message = "El nombre del grupo es obligatorio")
    @Size(max = 100)
    private String name;

    private String description;

    private GroupVisibility visibility;

    @Size(min = 4, max = 50, message = "La contraseña del grupo debe tener entre 4 y 50 caracteres")
    private String password;

    @Pattern(regexp = "^#[0-9a-fA-F]{6}$", message = "El color debe tener el formato #RRGGBB")
    private String color;
}
