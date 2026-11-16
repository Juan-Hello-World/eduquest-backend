package com.eduquest.api.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ForumVoteRequestDTO {
    @NotNull(message = "Indica tu voto")
    @Min(value = -1, message = "El voto debe ser -1, 0 o 1")
    @Max(value = 1, message = "El voto debe ser -1, 0 o 1")
    private Integer value;
}
