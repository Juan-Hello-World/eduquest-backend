package com.eduquest.api.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EditMessageRequestDTO {
    @Size(max = 1000, message = "El mensaje no puede superar los 1000 caracteres")
    private String content;
}
