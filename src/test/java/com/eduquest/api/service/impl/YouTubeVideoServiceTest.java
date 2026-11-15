package com.eduquest.api.service.impl;

import com.eduquest.api.exception.BadRequestException;
import com.eduquest.api.service.VideoMetadataService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class YouTubeVideoServiceTest {

    @Mock RestTemplate restTemplate;
    @InjectMocks YouTubeVideoService youTubeVideoService;

    @Test
    void resolve_existingVideo_returnsIdAndTitle() {
        when(restTemplate.getForEntity(anyString(), eq(Map.class), eq("https://youtu.be/dQw4w9WgXcQ")))
                .thenReturn(new ResponseEntity<>(Map.of("title", "Clase de cálculo"), HttpStatus.OK));

        VideoMetadataService.VideoInfo info = youTubeVideoService.resolve("https://www.youtube.com/watch?v=dQw4w9WgXcQ&t=30");

        assertThat(info.videoId()).isEqualTo("dQw4w9WgXcQ");
        assertThat(info.title()).isEqualTo("Clase de cálculo");
    }

    @Test
    void resolve_responseWithoutBody_returnsNullTitle() {
        when(restTemplate.getForEntity(anyString(), eq(Map.class), anyString()))
                .thenReturn(ResponseEntity.ok().build());

        assertThat(youTubeVideoService.resolve("https://youtu.be/dQw4w9WgXcQ").title()).isNull();
    }

    @Test
    void resolve_missingVideo_throwsBadRequest() {
        when(restTemplate.getForEntity(anyString(), eq(Map.class), anyString()))
                .thenThrow(new HttpClientErrorException(HttpStatus.NOT_FOUND));

        assertThatThrownBy(() -> youTubeVideoService.resolve("https://youtu.be/dQw4w9WgXcQ"))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void resolve_whenYouTubeIsUnreachable_keepsTheLink() {
        when(restTemplate.getForEntity(anyString(), eq(Map.class), anyString()))
                .thenThrow(new ResourceAccessException("timeout"));

        VideoMetadataService.VideoInfo info = youTubeVideoService.resolve("https://youtu.be/dQw4w9WgXcQ");

        assertThat(info.videoId()).isEqualTo("dQw4w9WgXcQ");
        assertThat(info.title()).isNull();
    }

    @Test
    void resolve_notYouTube_throwsBadRequest() {
        assertThatThrownBy(() -> youTubeVideoService.resolve("https://vimeo.com/123"))
                .isInstanceOf(BadRequestException.class);
    }
}
