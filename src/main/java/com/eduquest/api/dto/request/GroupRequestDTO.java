package com.eduquest.api.dto.request;

import jakarta.validation.constraints.NotBlank;
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
}