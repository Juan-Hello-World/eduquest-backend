package com.eduquest.api.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GeminiStudyPlanServiceTest {

    @Mock RestTemplate restTemplate;

    private GeminiStudyPlanService geminiStudyPlanService;

    @BeforeEach
    void setUp() {
        geminiStudyPlanService = new GeminiStudyPlanService(restTemplate);
        ReflectionTestUtils.setField(geminiStudyPlanService, "apiKey", "test-key");
        ReflectionTestUtils.setField(geminiStudyPlanService, "apiUrl",
                "https://generativelanguage.googleapis.com/v1beta/models");
        ReflectionTestUtils.setField(geminiStudyPlanService, "apiModel", "gemini-3.5-flash-lite");
    }

    @Test
    void generateStudyPlan_withCandidates_returnsTextAndUsesGeminiEndpoint() {
        Map<String, Object> thoughtPart = Map.of("text", "razonamiento interno", "thought", true);
        Map<String, Object> textPart = Map.of("text", "Plan de una semana...");
        Map<String, Object> content = Map.of("parts", List.of(thoughtPart, textPart));
        Map<String, Object> candidate = Map.of("content", content);
        Map<String, Object> body = Map.of("candidates", List.of(candidate));
        when(restTemplate.postForEntity(anyString(), any(), any(Class.class)))
                .thenReturn(new ResponseEntity<>((Map<String, Object>) body, (HttpHeaders) null, HttpStatus.OK));

        String plan = geminiStudyPlanService.generateStudyPlan("Calculo I");

        assertThat(plan).isEqualTo("Plan de una semana...");

        ArgumentCaptor<String> urlCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<HttpEntity> entityCaptor = ArgumentCaptor.forClass(HttpEntity.class);
        verify(restTemplate).postForEntity(urlCaptor.capture(), entityCaptor.capture(), any(Class.class));

        assertThat(urlCaptor.getValue()).isEqualTo(
                "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash-lite:generateContent");
        assertThat(entityCaptor.getValue().getHeaders().getFirst("x-goog-api-key")).isEqualTo("test-key");
    }

    @Test
    void generateStudyPlan_whenCutByTokenLimit_dropsUnfinishedLine() {
        Map<String, Object> textPart = Map.of("text", "Semana 1\n- Repasar límites\n**Sema");
        Map<String, Object> candidate = Map.of("content", Map.of("parts", List.of(textPart)), "finishReason", "MAX_TOKENS");
        Map<String, Object> body = Map.of("candidates", List.of(candidate));
        when(restTemplate.postForEntity(anyString(), any(), any(Class.class)))
                .thenReturn(new ResponseEntity<>((Map<String, Object>) body, (HttpHeaders) null, HttpStatus.OK));

        String plan = geminiStudyPlanService.generateStudyPlan("Calculo I");

        assertThat(plan).isEqualTo("Semana 1\n- Repasar límites");
    }

    @Test
    void generateStudyPlan_withEmptyCandidates_returnsFallbackPlan() {
        Map<String, Object> body = Map.of("candidates", List.of());
        when(restTemplate.postForEntity(anyString(), any(), any(Class.class)))
                .thenReturn(new ResponseEntity<>((Map<String, Object>) body, (HttpHeaders) null, HttpStatus.OK));

        String plan = geminiStudyPlanService.generateStudyPlan(
                "Curso: Algoritmo y Estructura de datos, Nivel de dificultad: Media");

        assertThat(plan).contains("Plan de estudio: Algoritmo y Estructura de datos");
        assertThat(plan).contains("Dificultad percibida: Media");
        assertThat(plan).contains("Semana 1");
    }

    @Test
    void generateStudyPlan_withNullBody_returnsFallbackPlan() {
        when(restTemplate.postForEntity(anyString(), any(), any(Class.class)))
                .thenReturn(new ResponseEntity<>((HttpHeaders) null, HttpStatus.INTERNAL_SERVER_ERROR));

        String plan = geminiStudyPlanService.generateStudyPlan(
                "Curso: Calculo I, Nivel de dificultad: Alta");

        assertThat(plan).contains("Plan de estudio: Calculo I");
        assertThat(plan).contains("Dificultad percibida: Alta");
        assertThat(plan).contains("Semana 6");
    }

    @Test
    void generateStudyPlan_whenServiceUnavailable_returnsFallbackPlan() {
        when(restTemplate.postForEntity(anyString(), any(), any(Class.class)))
                .thenThrow(new RestClientException("servicio no disponible"));

        String plan = geminiStudyPlanService.generateStudyPlan(
                "Curso: Fisica I, Nivel de dificultad: Baja");

        assertThat(plan).contains("Plan de estudio: Fisica I");
        assertThat(plan).contains("Dificultad percibida: Baja");
        assertThat(plan).contains("Semana 3");
    }

    @Test
    void generateStudyPlan_forCycleWhenServiceFails_returnsCombinedFallbackPlan() {
        when(restTemplate.postForEntity(anyString(), any(), any(Class.class)))
                .thenThrow(new RestClientException("servicio no disponible"));

        String plan = geminiStudyPlanService.generateStudyPlan(
                "Cursos del ciclo: Calculo I (Alta); Fisica I (baja)");

        assertThat(plan).contains("Plan de estudio del ciclo");
        assertThat(plan).contains("Cursos: Calculo I (Alta), Fisica I (Baja)");
        assertThat(plan).contains("Duración sugerida: 6 semanas");
        assertThat(plan).contains("- Dedicar 90 minutos al día a Calculo I");
        assertThat(plan).contains("- Fisica I: repasar");
        assertThat(plan).contains("Semana 6");
        assertThat(plan.substring(plan.indexOf("Semana 4"))).doesNotContain("Fisica I:");
    }

    @Test
    void generateStudyPlan_forMalformedCycle_usesSingleCourseFallback() {
        when(restTemplate.postForEntity(anyString(), any(), any(Class.class)))
                .thenThrow(new RestClientException("servicio no disponible"));

        String plan = geminiStudyPlanService.generateStudyPlan("Cursos del ciclo: sin dificultad");

        assertThat(plan).contains("Plan de estudio: Cursos del ciclo: sin dificultad");
    }
}
