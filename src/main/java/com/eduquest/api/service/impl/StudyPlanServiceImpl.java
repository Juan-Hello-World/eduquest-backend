package com.eduquest.api.service.impl;

import com.eduquest.api.dto.request.StudyPlanRequestDTO;
import com.eduquest.api.dto.response.StudyPlanResponseDTO;
import com.eduquest.api.entity.StudyPlan;
import com.eduquest.api.entity.User;
import com.eduquest.api.repository.StudyPlanRepository;
import com.eduquest.api.repository.UserRepository;
import com.eduquest.api.service.StudyPlanService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StudyPlanServiceImpl implements StudyPlanService {

    private final OpenAIServiceImpl openAIService;
    private final StudyPlanRepository studyPlanRepository;
    private final UserRepository userRepository;

    public StudyPlanServiceImpl(OpenAIServiceImpl openAIService,
                                StudyPlanRepository studyPlanRepository,
                                UserRepository userRepository) {
        this.openAIService = openAIService;
        this.studyPlanRepository = studyPlanRepository;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public StudyPlanResponseDTO generateStudyPlan(StudyPlanRequestDTO request, String username) {

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        // 2. Solicitar el contenido a la API de OpenAI combinando el curso y la dificultad
        String prompt = "Curso: " + request.getCourseName() + ", Nivel de dificultad: " + request.getDifficulty();
        String iaResponse = openAIService.generateStudyPlan(prompt);

        // 3. Crear la entidad y persistir en BD usando los nombres de tu diseño
        StudyPlan studyPlan = new StudyPlan();
        studyPlan.setCourseName(request.getCourseName()); // Soluciona la validación de courseName
        studyPlan.setDifficulty(request.getDifficulty()); // Soluciona la validación de difficulty

        // Dependiendo de cómo llamaste al texto en tu entidad StudyPlan, usa el setter correcto:
        // Puede ser studyPlan.setContent(iaResponse); o studyPlan.setGeneratedContent(iaResponse);
        studyPlan.setGeneratedContent(iaResponse);

        studyPlan.setUser(user);

        StudyPlan savedPlan = studyPlanRepository.save(studyPlan);

        // 4. Mapear la entidad guardada al DTO de respuesta
        return mapToResponseDTO(savedPlan);}

        private StudyPlanResponseDTO mapToResponseDTO(StudyPlan plan) {
            StudyPlanResponseDTO dto = new StudyPlanResponseDTO();

            dto.setId(plan.getId());
            dto.setCourseName(plan.getCourseName());
            dto.setDifficulty(plan.getDifficulty());
            // Ajusta el getter de acuerdo al nombre real en tu entidad
            dto.setGeneratedContent(plan.getGeneratedContent());
            dto.setCreatedAt(java.time.LocalDateTime.now());

            return dto;
        }
}