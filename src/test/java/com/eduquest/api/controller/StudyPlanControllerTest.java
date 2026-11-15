package com.eduquest.api.controller;

import com.eduquest.api.dto.request.StudyPlanRequestDTO;
import com.eduquest.api.dto.response.StudyPlanResponseDTO;
import com.eduquest.api.service.StudyPlanService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class StudyPlanControllerTest {

    @Mock StudyPlanService studyPlanService;

    @InjectMocks StudyPlanController studyPlanController;

    @Test
    void generatePlan_returnsCreated() {
        StudyPlanResponseDTO dto = new StudyPlanResponseDTO();
        dto.setId(1L);
        dto.setCourseName("Matematica Discreta");
        dto.setDifficulty("Alta");
        dto.setGeneratedContent("Plan detallado...");
        dto.setCreatedAt(LocalDateTime.now());

        Authentication authentication = mock(Authentication.class);
        when(authentication.getName()).thenReturn("alice");
        when(studyPlanService.generateStudyPlan(any(), anyString())).thenReturn(dto);

        StudyPlanRequestDTO request = new StudyPlanRequestDTO();
        request.setCourseName("Matematica Discreta");
        request.setDifficulty("Alta");

        ResponseEntity<StudyPlanResponseDTO> result = studyPlanController.generatePlan(request, authentication);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(result.getBody().getCourseName()).isEqualTo("Matematica Discreta");
    }

    @Test
    void getMyPlans_returnsOk() {
        Authentication authentication = mock(Authentication.class);
        when(authentication.getName()).thenReturn("alice");
        when(studyPlanService.getMyStudyPlans("alice")).thenReturn(java.util.List.of());

        ResponseEntity<java.util.List<StudyPlanResponseDTO>> result =
                studyPlanController.getMyPlans(authentication);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void updateProgress_returnsOk() {
        org.springframework.security.core.Authentication authentication =
                org.mockito.Mockito.mock(org.springframework.security.core.Authentication.class);
        org.mockito.Mockito.when(authentication.getName()).thenReturn("alice");
        StudyPlanResponseDTO dto = new StudyPlanResponseDTO();
        dto.setId(3L);
        org.mockito.Mockito.when(studyPlanService.updateProgress(
                org.mockito.ArgumentMatchers.eq(3L), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.eq("alice")))
                .thenReturn(dto);

        com.eduquest.api.dto.request.StudyPlanProgressRequestDTO request =
                new com.eduquest.api.dto.request.StudyPlanProgressRequestDTO();
        request.setCompletedSteps(java.util.Set.of(0));

        assertThat(studyPlanController.updateProgress(3L, request, authentication).getBody().getId()).isEqualTo(3L);
    }
}
