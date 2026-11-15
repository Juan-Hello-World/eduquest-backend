package com.eduquest.api.service;

import com.eduquest.api.dto.request.StudyPlanProgressRequestDTO;
import com.eduquest.api.dto.request.StudyPlanRequestDTO;
import com.eduquest.api.dto.response.StudyPlanResponseDTO;

import java.util.List;

public interface StudyPlanService {
    StudyPlanResponseDTO generateStudyPlan(StudyPlanRequestDTO request, String username);
    List<StudyPlanResponseDTO> getMyStudyPlans(String username);
    StudyPlanResponseDTO updateProgress(Long planId, StudyPlanProgressRequestDTO request, String username);
}