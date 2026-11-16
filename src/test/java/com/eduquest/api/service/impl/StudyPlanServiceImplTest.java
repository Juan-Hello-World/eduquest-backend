package com.eduquest.api.service.impl;

import com.eduquest.api.dto.request.StudyPlanRequestDTO;
import com.eduquest.api.dto.response.StudyPlanResponseDTO;
import com.eduquest.api.entity.StudyPlan;
import com.eduquest.api.entity.User;
import com.eduquest.api.exception.InvalidOperationException;
import com.eduquest.api.exception.ResourceNotFoundException;
import com.eduquest.api.repository.StudyPlanRepository;
import com.eduquest.api.repository.UserRepository;
import com.eduquest.api.service.StudyPlanGenerationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.context.ApplicationEventPublisher;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class StudyPlanServiceImplTest {

    @Mock StudyPlanGenerationService generationService;
    @Mock StudyPlanRepository studyPlanRepository;
    @Mock UserRepository userRepository;
    @Mock ApplicationEventPublisher eventPublisher;

    @InjectMocks StudyPlanServiceImpl studyPlanService;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setUsername("alice");
        user.setEmail("alice@utec.edu.pe");
    }

    @Test
    void generateStudyPlan_withValidAiResponse_persistsAndPublishesEvent() {
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        when(generationService.generateStudyPlan(any())).thenReturn("Plan de estudio detallado...");
        when(studyPlanRepository.save(any(StudyPlan.class))).thenAnswer(invocation -> {
            StudyPlan saved = invocation.getArgument(0);
            saved.setId(1L);
            return saved;
        });

        StudyPlanRequestDTO request = new StudyPlanRequestDTO();
        request.setCourseName("Matematica Discreta");
        request.setDifficulty("Alta");

        StudyPlanResponseDTO response = studyPlanService.generateStudyPlan(request, "alice");

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getCourseName()).isEqualTo("Matematica Discreta");
        assertThat(response.getDifficulty()).isEqualTo("Alta");
        assertThat(response.getGeneratedContent()).contains("detallado");
        verify(eventPublisher).publishEvent(any());
    }

    @Test
    void generateStudyPlan_withBlankAiResponse_throwsInvalidOperation() {
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        when(generationService.generateStudyPlan(any())).thenReturn("   ");

        StudyPlanRequestDTO request = new StudyPlanRequestDTO();
        request.setCourseName("Matematica Discreta");
        request.setDifficulty("Alta");

        assertThatThrownBy(() -> studyPlanService.generateStudyPlan(request, "alice"))
                .isInstanceOf(InvalidOperationException.class);
    }

    @Test
    void generateStudyPlan_withUnknownUser_throwsNotFound() {
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        StudyPlanRequestDTO request = new StudyPlanRequestDTO();
        request.setCourseName("Matematica Discreta");
        request.setDifficulty("Alta");

        assertThatThrownBy(() -> studyPlanService.generateStudyPlan(request, "ghost"))
                .isInstanceOf(ResourceNotFoundException.class);
    }
    @Test
    void getMyStudyPlans_returnsUserPlans() {
        StudyPlan plan = new StudyPlan();
        plan.setId(1L);
        plan.setCourseName("Algoritmos");
        plan.setDifficulty("Alta");
        plan.setGeneratedContent("Contenido");

        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        when(studyPlanRepository.findByUserId(1L)).thenReturn( java.util.List.of(plan));

        java.util.List<StudyPlanResponseDTO> plans = studyPlanService.getMyStudyPlans("alice");

        assertThat(plans).hasSize(1);
        assertThat(plans.get(0).getCourseName()).isEqualTo("Algoritmos");
    }

    @Test
    void generateStudyPlan_withSeveralCourses_buildsCyclePrompt() {
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        when(generationService.generateStudyPlan(any())).thenReturn("Semana 1\n- Repasar");
        when(studyPlanRepository.save(any(StudyPlan.class))).thenAnswer(invocation -> invocation.getArgument(0));

        StudyPlanRequestDTO request = new StudyPlanRequestDTO();
        request.setCourses(java.util.List.of(
                new com.eduquest.api.dto.request.CourseInputDTO("Calculo", "alta"),
                new com.eduquest.api.dto.request.CourseInputDTO("Fisica", "Media")));

        StudyPlanResponseDTO response = studyPlanService.generateStudyPlan(request, "alice");

        verify(generationService).generateStudyPlan("Cursos del ciclo: Calculo (Alta); Fisica (Media)");
        assertThat(response.getCourseName()).isEqualTo("Calculo, Fisica");
        assertThat(response.getDifficulty()).isEqualTo("Mixta");
        assertThat(response.getTotalSteps()).isEqualTo(1);
    }

    @Test
    void generateStudyPlan_withLongCourseList_abbreviatesName() {
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        when(generationService.generateStudyPlan(any())).thenReturn("Plan");
        when(studyPlanRepository.save(any(StudyPlan.class))).thenAnswer(invocation -> invocation.getArgument(0));

        String longName = "Curso con un nombre bastante largo para probar";
        StudyPlanRequestDTO request = new StudyPlanRequestDTO();
        request.setCourses(java.util.List.of(
                new com.eduquest.api.dto.request.CourseInputDTO(longName, "Baja"),
                new com.eduquest.api.dto.request.CourseInputDTO(longName, "Baja"),
                new com.eduquest.api.dto.request.CourseInputDTO(longName, "Baja")));

        StudyPlanResponseDTO response = studyPlanService.generateStudyPlan(request, "alice");

        assertThat(response.getCourseName()).hasSize(100).endsWith("...");
    }

    @Test
    void generateStudyPlan_withoutCourses_throwsInvalidOperation() {
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));

        StudyPlanRequestDTO request = new StudyPlanRequestDTO();

        assertThat(request.isCourseProvided()).isFalse();
        assertThatThrownBy(() -> studyPlanService.generateStudyPlan(request, "alice"))
                .isInstanceOf(InvalidOperationException.class);
    }

    @Test
    void updateProgress_keepsOnlyValidStepIndexes() {
        StudyPlan plan = new StudyPlan();
        plan.setId(3L);
        plan.setGeneratedContent("Semana 1\n- Leer\n- Practicar\n1. Evaluar");
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        when(studyPlanRepository.findByIdAndUserId(3L, 1L)).thenReturn(Optional.of(plan));
        when(studyPlanRepository.save(any(StudyPlan.class))).thenAnswer(invocation -> invocation.getArgument(0));

        com.eduquest.api.dto.request.StudyPlanProgressRequestDTO request =
                new com.eduquest.api.dto.request.StudyPlanProgressRequestDTO();
        request.setCompletedSteps(new java.util.HashSet<>(java.util.Arrays.asList(2, 0, 7, -1, null)));

        StudyPlanResponseDTO response = studyPlanService.updateProgress(3L, request, "alice");

        assertThat(response.getTotalSteps()).isEqualTo(3);
        assertThat(response.getCompletedSteps()).containsExactly(0, 2);
    }

    @Test
    void updateProgress_planOfAnotherUser_throwsNotFound() {
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        when(studyPlanRepository.findByIdAndUserId(3L, 1L)).thenReturn(Optional.empty());

        com.eduquest.api.dto.request.StudyPlanProgressRequestDTO request =
                new com.eduquest.api.dto.request.StudyPlanProgressRequestDTO();
        request.setCompletedSteps(java.util.Set.of(0));

        assertThatThrownBy(() -> studyPlanService.updateProgress(3L, request, "alice"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void deletePlan_ownPlanOnly() {
        com.eduquest.api.entity.StudyPlan plan = new com.eduquest.api.entity.StudyPlan();
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        when(studyPlanRepository.findByIdAndUserId(3L, 1L)).thenReturn(Optional.of(plan));
        when(studyPlanRepository.findByIdAndUserId(4L, 1L)).thenReturn(Optional.empty());

        studyPlanService.deletePlan(3L, "alice");

        verify(studyPlanRepository).delete(plan);
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> studyPlanService.deletePlan(4L, "alice"))
                .isInstanceOf(com.eduquest.api.exception.ResourceNotFoundException.class);
    }
}
