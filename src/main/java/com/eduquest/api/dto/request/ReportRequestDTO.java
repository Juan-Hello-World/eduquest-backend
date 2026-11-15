package com.eduquest.api.dto.request;

import com.eduquest.api.entity.ReportTargetType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReportRequestDTO {
    @NotNull(message = "Indica qué estás reportando")
    private ReportTargetType targetType;

    @NotNull(message = "Indica qué estás reportando")
    private Long targetId;

    @NotBlank(message = "Cuéntanos el motivo del reporte")
    @Size(min = 10, max = 500, message = "El motivo debe tener entre 10 y 500 caracteres")
    private String reason;
}
