package com.eduquest.api.service.impl;

import com.eduquest.api.dto.request.StudyPlanRequestDTO;
import com.eduquest.api.dto.response.StudyPlanResponseDTO;
import com.eduquest.api.entity.StudyPlan;
import com.eduquest.api.entity.User;
import com.eduquest.api.event.OnStudyPlanGeneratedEvent;
import com.eduquest.api.exception.InvalidOperationException;
import com.eduquest.api.exception.ResourceNotFoundException;
import com.eduquest.api.repository.StudyPlanRepository;
import com.eduquest.api.repository.UserRepository;
import com.eduquest.api.service.OpenAIService;
import com.eduquest.api.service.StudyPlanService;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StudyPlanServiceImpl implements StudyPlanService {

    private final OpenAIService openAIService;
    private final StudyPlanRepository studyPlanRepository;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;

    public StudyPlanServiceImpl(OpenAIService openAIService,
                                StudyPlanRepository studyPlanRepository,
                                UserRepository userRepository,
                                ApplicationEventPublisher eventPublisher) {
        this.openAIService = openAIService;
        this.studyPlanRepository = studyPlanRepository;
        this.userRepository = userRepository;
        this.eventPublisher = eventPublisher;
    }

    @Override
    @Transactional
    public StudyPlanResponseDTO generateStudyPlan(StudyPlanRequestDTO request, String username) {

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        String prompt = "Curso: " + request.getCourseName() + ", Nivel de dificultad: " + request.getDifficulty();
        String iaResponse = openAIService.generateStudyPlan(prompt);

        if (iaResponse == null || iaResponse.isBlank()) {
            throw new InvalidOperationException("No se pudo generar un plan de estudio válido para el curso indicado");
        }

        StudyPlan studyPlan = new StudyPlan();
        studyPlan.setCourseName(request.getCourseName());
        studyPlan.setDifficulty(request.getDifficulty());
        studyPlan.setGeneratedContent(iaResponse);
        studyPlan.setUser(user);

        StudyPlan savedPlan = studyPlanRepository.save(studyPlan);

        eventPublisher.publishEvent(new OnStudyPlanGeneratedEvent(
                this, username, request.getCourseName(), request.getDifficulty()));

        return mapToResponseDTO(savedPlan);
    }

    private StudyPlanResponseDTO mapToResponseDTO(StudyPlan plan) {
        StudyPlanResponseDTO dto = new StudyPlanResponseDTO();
        dto.setId(plan.getId());
        dto.setCourseName(plan.getCourseName());
        dto.setDifficulty(plan.getDifficulty());
        dto.setGeneratedContent(plan.getGeneratedContent());
        dto.setCreatedAt(plan.getCreatedAt());
        return dto;
    }
}