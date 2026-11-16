package com.eduquest.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class RoomRequestDTO {
    @NotBlank(message = "Ponle un nombre al chat")
    @Size(max = 60, message = "El nombre puede tener hasta 60 caracteres")
    private String name;

    @NotNull(message = "Elige a las personas del chat")
    @Size(min = 2, max = 9, message = "Un chat grupal tiene entre 3 y 10 personas contándote")
    private List<Long> memberIds;
}
