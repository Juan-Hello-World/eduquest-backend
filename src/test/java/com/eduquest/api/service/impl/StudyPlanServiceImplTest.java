package com.eduquest.api.service.impl;

import com.eduquest.api.dto.request.StudyPlanRequestDTO;
import com.eduquest.api.dto.response.StudyPlanResponseDTO;
import com.eduquest.api.entity.StudyPlan;
import com.eduquest.api.entity.User;
import com.eduquest.api.exception.InvalidOperationException;
import com.eduquest.api.exception.ResourceNotFoundException;
import com.eduquest.api.repository.StudyPlanRepository;
import com.eduquest.api.repository.UserRepository;
import com.eduquest.api.service.OpenAIService;
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

    @Mock OpenAIService openAIService;
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
        when(openAIService.generateStudyPlan(any())).thenReturn("Plan de estudio detallado...");
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
        when(openAIService.generateStudyPlan(any())).thenReturn("   ");

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
}