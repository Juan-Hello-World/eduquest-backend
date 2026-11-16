package com.eduquest.api.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class RoomUpdateRequestDTO {
    @Size(min = 1, max = 60, message = "El nombre debe tener entre 1 y 60 caracteres")
    private String name;

    @Size(max = 9, message = "Puedes agregar hasta 9 personas a la vez")
    private List<Long> addMemberIds;
}
