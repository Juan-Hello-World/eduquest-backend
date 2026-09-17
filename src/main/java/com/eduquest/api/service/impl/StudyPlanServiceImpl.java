package com.eduquest.api.service.impl;

import com.eduquest.api.dto.request.StudyPlanRequestDTO;
import com.eduquest.api.dto.response.StudyPlanResponseDTO;
import com.eduquest.api.entity.StudyPlan;
import com.eduquest.api.entity.User;
import com.eduquest.api.exception.ExternalServiceException;
import com.eduquest.api.exception.ResourceNotFoundException;
import com.eduquest.api.repository.StudyPlanRepository;
import com.eduquest.api.repository.UserRepository;
import com.eduquest.api.service.StudyPlanService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class StudyPlanServiceImpl implements StudyPlanService {

    private final StudyPlanRepository studyPlanRepository;
    private final UserRepository userRepository;
    private final RestTemplate restTemplate;

    @Value("${openai.api.key}")
    private String openAiApiKey;

    public StudyPlanServiceImpl(StudyPlanRepository studyPlanRepository, UserRepository userRepository, RestTemplate restTemplate) {
        this.studyPlanRepository = studyPlanRepository;
        this.userRepository = userRepository;
        this.restTemplate = restTemplate;
    }

    @Override
    @Transactional
    public StudyPlanResponseDTO generateStudyPlan(StudyPlanRequestDTO request, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        //Comentado temporalmente hasta tener el API
        // String aiResponse = callOpenAiApi(request.getCourseName(), request.getDifficulty());

        // Mock Temporal
        String aiResponse = "PLAN DE ESTUDIO GENERADO (SIMULACIÓN)\n" +
                "Curso: " + request.getCourseName() + "\n" +
                "Dificultad adaptada: " + request.getDifficulty() + "\n\n" +
                "Semana 1: Fundamentos teóricos.\n" +
                "Semana 2: Resolución de ejercicios prácticos.\n" +
                "Semana 3: Proyecto de integración.";

        StudyPlan studyPlan = new StudyPlan();
        studyPlan.setCourseName(request.getCourseName());
        studyPlan.setDifficulty(request.getDifficulty());
        studyPlan.setGeneratedContent(aiResponse);
        studyPlan.setUser(user);

        StudyPlan savedPlan = studyPlanRepository.save(studyPlan);

        StudyPlanResponseDTO response = new StudyPlanResponseDTO();
        response.setId(savedPlan.getId());
        response.setCourseName(savedPlan.getCourseName());
        response.setDifficulty(savedPlan.getDifficulty());
        response.setGeneratedContent(savedPlan.getGeneratedContent());
        response.setCreatedAt(savedPlan.getCreatedAt());

        return response;
    }

    private String callOpenAiApi(String course, String difficulty) {
        String url = "https://api.openai.com/v1/chat/completions";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(openAiApiKey);

        String prompt = String.format("Actúa como un tutor académico experto. Crea un plan de estudio estructurado por semanas para el tema '%s'. El estudiante percibe la dificultad de este tema como '%s'. Ajusta la metodología y el nivel de detalle de los conceptos a esta dificultad. Devuelve solo el plan en formato de texto claro.", course, difficulty);

        Map<String, Object> message = new HashMap<>();
        message.put("role", "user");
        message.put("content", prompt);

        Map<String, Object> body = new HashMap<>();
        body.put("model", "gpt-3.5-turbo");
        body.put("messages", List.of(message));
        body.put("max_tokens", 500); // Límite definido para no sobrecargar costos y recursos

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(url, entity, Map.class);
            Map<String, Object> responseBody = response.getBody();

            if (responseBody != null && responseBody.containsKey("choices")) {
                List<Map<String, Object>> choices = (List<Map<String, Object>>) responseBody.get("choices");
                Map<String, Object> firstChoice = choices.get(0);
                Map<String, Object> messageMap = (Map<String, Object>) firstChoice.get("message");
                return (String) messageMap.get("content");
            }
            throw new ExternalServiceException("Respuesta inválida de la API de OpenAI");
        } catch (Exception e) {
            throw new ExternalServiceException("Error al comunicarse con la IA de Plan de Estudios: " + e.getMessage());
        }
    }
}