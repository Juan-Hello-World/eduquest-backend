package com.eduquest.api.controller;

import com.eduquest.api.dto.request.ReviewRequestDTO;
import com.eduquest.api.dto.response.PageResponseDTO;
import com.eduquest.api.dto.response.ReviewResponseDTO;
import com.eduquest.api.security.UserPrincipal;
import com.eduquest.api.service.ReviewService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ReviewControllerTest {

    @Mock ReviewService reviewService;

    @InjectMocks ReviewController reviewController;

    private UserPrincipal principal() {
        return new UserPrincipal(1L, "alice", "alice@utec.edu.pe", "hash",
                List.of(new SimpleGrantedAuthority("ROLE_USER")));
    }

    private ReviewResponseDTO response() {
        ReviewResponseDTO dto = new ReviewResponseDTO();
        dto.setId(1L);
        dto.setRating(5);
        dto.setComment("Excelente");
        dto.setCreatedAt(LocalDateTime.now());
        dto.setAuthorUsername("alice");
        dto.setDocumentTitle("Apuntes");
        return dto;
    }

    @Test
    void createReview_returnsCreated() {
        when(reviewService.createReview(any(), anyString())).thenReturn(response());

        ReviewRequestDTO request = new ReviewRequestDTO();
        request.setDocumentId(1L);
        request.setRating(5);
        request.setComment("Excelente");

        ResponseEntity<ReviewResponseDTO> result = reviewController.createReview(request, principal());

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(result.getBody().getComment()).isEqualTo("Excelente");
    }

    @Test
    void getReviewsByDocument_returnsPaginatedResponse() {
        PageResponseDTO<ReviewResponseDTO> page =
                new PageResponseDTO<>(List.of(response()), 0, 10, 1, 1);
        when(reviewService.getReviewsByDocument(anyLong(), any())).thenReturn(page);

        ResponseEntity<PageResponseDTO<ReviewResponseDTO>> result = reviewController.getReviewsByDocument(1L, 0, 10);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody().content()).hasSize(1);
    }

    @Test
    void getReview_returnsOk() {
        when(reviewService.getReview(anyLong())).thenReturn(response());

        ResponseEntity<ReviewResponseDTO> result = reviewController.getReview(1L);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody().getId()).isEqualTo(1L);
    }

    @Test
    void deleteReview_asPrincipal_returnsNoContent() {
        ResponseEntity<Void> result = reviewController.deleteReview(1L, principal());

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(reviewService).deleteReview(1L, "alice");
    }
}