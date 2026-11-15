package com.eduquest.api.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.Set;

@Getter
@Setter
public class StudyPlanProgressRequestDTO {
    @NotNull(message = "Indica los pasos completados")
    private Set<Integer> completedSteps;
}
