package com.eduquest.api.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class GeminiClient {

    private static final Logger log = LoggerFactory.getLogger(GeminiClient.class);

    private final RestTemplate restTemplate;

    @Value("${gemini.api.key}")
    private String apiKey;

    @Value("${gemini.api.url}")
    private String apiUrl;

    @Value("${gemini.api.model:gemini-3.5-flash-lite}")
    private String apiModel;

    public GeminiClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public static Map<String, Object> text(String value) {
        return Map.of("text", value);
    }

    public static Map<String, Object> inlineData(String mimeType, String base64) {
        return Map.of("inline_data", Map.of("mime_type", mimeType, "data", base64));
    }

    @SuppressWarnings("unchecked")
    public Optional<String> generate(String instruction, List<Map<String, Object>> parts, int maxTokens, boolean json) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("x-goog-api-key", apiKey);

            Map<String, Object> config = new HashMap<>();
            config.put("maxOutputTokens", maxTokens);
            config.put("temperature", 0.4);
            if (json) {
                config.put("responseMimeType", "application/json");
            }
            Map<String, Object> body = new HashMap<>();
            body.put("systemInstruction", Map.of("parts", List.of(text(instruction))));
            body.put("contents", List.of(Map.of("role", "user", "parts", parts)));
            body.put("generationConfig", config);

            ResponseEntity<Map> response = restTemplate.postForEntity(
                    apiUrl + "/" + apiModel + ":generateContent", new HttpEntity<>(body, headers), Map.class);
            Map<String, Object> payload = response.getBody();
            if (payload == null || !(payload.get("candidates") instanceof List<?> candidates) || candidates.isEmpty()) {
                return Optional.empty();
            }
            Map<String, Object> content = (Map<String, Object>) ((Map<String, Object>) candidates.getFirst()).get("content");
            if (content == null || !(content.get("parts") instanceof List<?> responseParts)) {
                return Optional.empty();
            }
            StringBuilder result = new StringBuilder();
            for (Object item : responseParts) {
                Map<String, Object> part = (Map<String, Object>) item;
                if (!Boolean.TRUE.equals(part.get("thought")) && part.get("text") instanceof String value) {
                    result.append(value);
                }
            }
            String output = result.toString().trim();
            return output.isEmpty() ? Optional.empty() : Optional.of(output);
        } catch (RestClientException | ClassCastException ex) {
            log.warn("No fue posible obtener una respuesta del servicio de IA: {}", ex.getMessage());
            return Optional.empty();
        }
    }
}
