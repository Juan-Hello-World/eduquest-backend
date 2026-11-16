package com.eduquest.api.service.impl;

import com.eduquest.api.dto.request.CourseInputDTO;
import com.eduquest.api.dto.request.StudyPlanProgressRequestDTO;
import com.eduquest.api.dto.request.StudyPlanRequestDTO;
import com.eduquest.api.dto.response.StudyPlanResponseDTO;
import com.eduquest.api.entity.StudyPlan;
import com.eduquest.api.entity.User;
import com.eduquest.api.event.OnStudyPlanGeneratedEvent;
import com.eduquest.api.event.OnUserActivityEvent;
import com.eduquest.api.exception.InvalidOperationException;
import com.eduquest.api.exception.ResourceNotFoundException;
import com.eduquest.api.repository.StudyPlanRepository;
import com.eduquest.api.repository.UserRepository;
import com.eduquest.api.service.PlanSteps;
import com.eduquest.api.service.StudyPlanGenerationService;
import com.eduquest.api.service.StudyPlanService;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class StudyPlanServiceImpl implements StudyPlanService {

    static final String CYCLE_DIFFICULTY = "Mixta";
    private static final int MAX_COURSE_NAME = 100;

    private final StudyPlanGenerationService generationService;
    private final StudyPlanRepository studyPlanRepository;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;

    public StudyPlanServiceImpl(StudyPlanGenerationService generationService,
                                StudyPlanRepository studyPlanRepository,
                                UserRepository userRepository,
                                ApplicationEventPublisher eventPublisher) {
        this.generationService = generationService;
        this.studyPlanRepository = studyPlanRepository;
        this.userRepository = userRepository;
        this.eventPublisher = eventPublisher;
    }

    @Override
    @Transactional
    public StudyPlanResponseDTO generateStudyPlan(StudyPlanRequestDTO request, String username) {
        User user = findUser(username);

        List<CourseInputDTO> courses = request.resolveCourses();
        if (courses.isEmpty()) {
            throw new InvalidOperationException("Indica al menos un curso con su dificultad");
        }

        String prompt;
        String courseName;
        String difficulty;
        if (courses.size() == 1) {
            CourseInputDTO course = courses.getFirst();
            courseName = course.getName().trim();
            difficulty = normalizeDifficulty(course.getDifficulty());
            prompt = "Curso: " + courseName + ", Nivel de dificultad: " + difficulty;
        } else {
            prompt = "Cursos del ciclo: " + courses.stream()
                    .map(course -> course.getName().trim() + " (" + normalizeDifficulty(course.getDifficulty()) + ")")
                    .collect(Collectors.joining("; "));
            courseName = abbreviate(courses.stream()
                    .map(course -> course.getName().trim())
                    .collect(Collectors.joining(", ")));
            difficulty = CYCLE_DIFFICULTY;
        }

        String generatedContent = generationService.generateStudyPlan(prompt);
        if (generatedContent == null || generatedContent.isBlank()) {
            throw new InvalidOperationException("No se pudo generar un plan de estudio válido para el curso indicado");
        }

        StudyPlan studyPlan = new StudyPlan();
        studyPlan.setCourseName(courseName);
        studyPlan.setDifficulty(difficulty);
        studyPlan.setGeneratedContent(generatedContent);
        studyPlan.setUser(user);

        StudyPlan savedPlan = studyPlanRepository.save(studyPlan);

        eventPublisher.publishEvent(new OnStudyPlanGeneratedEvent(this, username, courseName, difficulty));

        return mapToResponseDTO(savedPlan);
    }

    @Override
    @Transactional(readOnly = true)
    public List<StudyPlanResponseDTO> getMyStudyPlans(String username) {
        User user = findUser(username);
        return studyPlanRepository.findByUserId(user.getId()).stream()
                .map(this::mapToResponseDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public StudyPlanResponseDTO updateProgress(Long planId, StudyPlanProgressRequestDTO request, String username) {
        User user = findUser(username);
        StudyPlan plan = studyPlanRepository.findByIdAndUserId(planId, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Plan de estudio no encontrado"));

        int totalSteps = PlanSteps.count(plan.getGeneratedContent());
        Set<Integer> valid = request.getCompletedSteps().stream()
                .filter(step -> step != null && step >= 0 && step < totalSteps)
                .collect(Collectors.toSet());

        plan.getCompletedSteps().clear();
        plan.getCompletedSteps().addAll(valid);
        StudyPlan saved = studyPlanRepository.save(plan);
        eventPublisher.publishEvent(new OnUserActivityEvent(this, user.getId()));
        return mapToResponseDTO(saved);
    }

    @Override
    @Transactional
    public void deletePlan(Long planId, String username) {
        User user = findUser(username);
        StudyPlan plan = studyPlanRepository.findByIdAndUserId(planId, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Plan de estudio no encontrado"));
        studyPlanRepository.delete(plan);
    }

    private User findUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
    }

    private static String normalizeDifficulty(String difficulty) {
        String clean = difficulty.trim().toLowerCase(Locale.ROOT);
        return Character.toUpperCase(clean.charAt(0)) + clean.substring(1);
    }

    private static String abbreviate(String text) {
        return text.length() <= MAX_COURSE_NAME ? text : text.substring(0, MAX_COURSE_NAME - 3) + "...";
    }

    private StudyPlanResponseDTO mapToResponseDTO(StudyPlan plan) {
        StudyPlanResponseDTO dto = new StudyPlanResponseDTO();
        dto.setId(plan.getId());
        dto.setCourseName(plan.getCourseName());
        dto.setDifficulty(plan.getDifficulty());
        dto.setGeneratedContent(plan.getGeneratedContent());
        dto.setCreatedAt(plan.getCreatedAt());
        dto.setTotalSteps(PlanSteps.count(plan.getGeneratedContent()));
        dto.setCompletedSteps(plan.getCompletedSteps() == null ? List.of()
                : plan.getCompletedSteps().stream().sorted().toList());
        return dto;
    }
}
