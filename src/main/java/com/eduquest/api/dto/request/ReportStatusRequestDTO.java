package com.eduquest.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReportStatusRequestDTO {
    @NotBlank(message = "El estado es obligatorio")
    @Pattern(regexp = "^(RESOLVED|DISMISSED)$", message = "El estado debe ser RESOLVED o DISMISSED")
    private String status;
}
