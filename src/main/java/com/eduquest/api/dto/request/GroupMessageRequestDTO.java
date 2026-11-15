package com.eduquest.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GroupMessageRequestDTO {
    @NotBlank(message = "El mensaje no puede estar vacío")
    @Size(max = 1000, message = "El mensaje no puede superar los 1000 caracteres")
    private String content;
}
