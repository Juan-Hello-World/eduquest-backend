package com.eduquest.api.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class StudyPlanRequestDTO {
    @Size(max = 100, message = "El tema no puede superar los 100 caracteres")
    private String courseName;

    @Pattern(regexp = "^(?i)(Baja|Media|Alta)$", message = "La dificultad debe ser Baja, Media o Alta")
    private String difficulty;

    @Valid
    @Size(max = 8, message = "Puedes incluir hasta 8 cursos por plan")
    private List<CourseInputDTO> courses = new ArrayList<>();

    @AssertTrue(message = "Indica al menos un curso con su dificultad")
    public boolean isCourseProvided() {
        return !resolveCourses().isEmpty();
    }

    public List<CourseInputDTO> resolveCourses() {
        if (courses != null && !courses.isEmpty()) {
            return courses;
        }
        if (courseName != null && !courseName.isBlank() && difficulty != null && !difficulty.isBlank()) {
            return List.of(new CourseInputDTO(courseName, difficulty));
        }
        return List.of();
    }
}
