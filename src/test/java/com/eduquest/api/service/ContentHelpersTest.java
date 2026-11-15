package com.eduquest.api.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ContentHelpersTest {

    @Test
    void extractVideoId_supportsCommonYouTubeFormats() {
        assertThat(YouTubeLinks.extractVideoId("https://www.youtube.com/watch?v=dQw4w9WgXcQ")).contains("dQw4w9WgXcQ");
        assertThat(YouTubeLinks.extractVideoId("https://youtube.com/watch?feature=share&v=dQw4w9WgXcQ")).contains("dQw4w9WgXcQ");
        assertThat(YouTubeLinks.extractVideoId("https://youtu.be/dQw4w9WgXcQ?si=abc")).contains("dQw4w9WgXcQ");
        assertThat(YouTubeLinks.extractVideoId("https://m.youtube.com/shorts/dQw4w9WgXcQ")).contains("dQw4w9WgXcQ");
        assertThat(YouTubeLinks.extractVideoId("https://www.youtube.com/live/dQw4w9WgXcQ")).contains("dQw4w9WgXcQ");
    }

    @Test
    void extractVideoId_rejectsOtherLinks() {
        assertThat(YouTubeLinks.extractVideoId(null)).isEmpty();
        assertThat(YouTubeLinks.extractVideoId("https://vimeo.com/12345")).isEmpty();
        assertThat(YouTubeLinks.extractVideoId("https://www.youtube.com/watch?v=corto")).isEmpty();
    }

    @Test
    void countSteps_countsBulletsAndNumberedLinesOnly() {
        String plan = """
                Plan de estudio
                ---
                - Leer el capítulo 1
                * Hacer resumen
                • Repasar
                1. Resolver ejercicios
                2) Autoevaluación
                **Semana 2**
                -sin espacio
                """;

        assertThat(PlanSteps.count(plan)).isEqualTo(5);
        assertThat(PlanSteps.count(null)).isZero();
        assertThat(PlanSteps.count("  ")).isZero();
    }
}
