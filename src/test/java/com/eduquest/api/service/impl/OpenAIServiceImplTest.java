package com.eduquest.api.service.impl;

import com.eduquest.api.exception.ExternalServiceException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OpenAIServiceImplTest {

    @Mock RestTemplate restTemplate;

    private OpenAIServiceImpl openAIService;

    @BeforeEach
    void setUp() {
        openAIService = new OpenAIServiceImpl(restTemplate);
        ReflectionTestUtils.setField(openAIService, "apiKey", "test-key");
        ReflectionTestUtils.setField(openAIService, "apiUrl", "https://api.openai.com/v1/chat/completions");
    }

    @Test
    void generateStudyPlan_withChoices_returnsContent() {
        Map<String, Object> message = Map.of("role", "assistant", "content", "Plan de una semana...");
        Map<String, Object> choice = Map.of("message", message);
        Map<String, Object> body = Map.of("choices", List.of(choice));
        when(restTemplate.postForEntity(anyString(), any(), any(Class.class)))
                .thenReturn(new ResponseEntity<>((Map<String, Object>) body, (HttpHeaders) null, HttpStatus.OK));

        String plan = openAIService.generateStudyPlan("Calculo I");

        assertThat(plan).isEqualTo("Plan de una semana...");
    }

    @Test
    void generateStudyPlan_withEmptyChoices_throwsExternalService() {
        Map<String, Object> body = Map.of("choices", List.of());
        when(restTemplate.postForEntity(anyString(), any(), any(Class.class)))
                .thenReturn(new ResponseEntity<>((Map<String, Object>) body, (HttpHeaders) null, HttpStatus.OK));

        assertThatThrownBy(() -> openAIService.generateStudyPlan("Calculo I"))
                .isInstanceOf(ExternalServiceException.class);
    }

    @Test
    void generateStudyPlan_withNullBody_throwsExternalService() {
        when(restTemplate.postForEntity(anyString(), any(), any(Class.class)))
                .thenReturn(new ResponseEntity<>((HttpHeaders) null, HttpStatus.INTERNAL_SERVER_ERROR));

        assertThatThrownBy(() -> openAIService.generateStudyPlan("Calculo I"))
                .isInstanceOf(ExternalServiceException.class);
    }
}