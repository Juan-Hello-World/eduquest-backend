package com.eduquest.api.service.impl;

import com.eduquest.api.exception.BadRequestException;
import com.eduquest.api.service.VideoMetadataService;
import com.eduquest.api.service.YouTubeLinks;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
public class YouTubeVideoService implements VideoMetadataService {

    private static final Logger log = LoggerFactory.getLogger(YouTubeVideoService.class);
    private static final String OEMBED_URL = "https://www.youtube.com/oembed?format=json&url={url}";

    private final RestTemplate restTemplate;

    public YouTubeVideoService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Override
    public VideoInfo resolve(String url) {
        String videoId = YouTubeLinks.extractVideoId(url)
                .orElseThrow(() -> new BadRequestException("Pega un enlace de YouTube válido (youtube.com o youtu.be)"));
        try {
            ResponseEntity<Map> response = restTemplate.getForEntity(OEMBED_URL, Map.class, "https://youtu.be/" + videoId);
            Map<?, ?> body = response.getBody();
            Object title = body == null ? null : body.get("title");
            return new VideoInfo(videoId, title instanceof String text ? text : null);
        } catch (HttpClientErrorException ex) {
            throw new BadRequestException("Ese video de YouTube no existe o es privado");
        } catch (RestClientException ex) {
            log.warn("No se pudo validar el video {} con YouTube: {}", videoId, ex.getMessage());
            return new VideoInfo(videoId, null);
        }
    }
}
