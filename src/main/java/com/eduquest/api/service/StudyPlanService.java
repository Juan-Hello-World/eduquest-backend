package com.eduquest.api.service;

import com.eduquest.api.dto.request.StudyPlanRequestDTO;
import com.eduquest.api.dto.response.StudyPlanResponseDTO;

public interface StudyPlanService {
    StudyPlanResponseDTO generateStudyPlan(StudyPlanRequestDTO request, String username);
}