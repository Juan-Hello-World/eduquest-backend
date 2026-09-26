package com.eduquest.api.service;

import com.eduquest.api.dto.request.ReviewRequestDTO;
import com.eduquest.api.dto.response.PageResponseDTO;
import com.eduquest.api.dto.response.ReviewResponseDTO;
import org.springframework.data.domain.Pageable;

public interface ReviewService {
    ReviewResponseDTO createReview(ReviewRequestDTO request, String username);
    PageResponseDTO<ReviewResponseDTO> getReviewsByDocument(Long documentId, Pageable pageable);
    ReviewResponseDTO getReview(Long id);
    void deleteReview(Long id, String username);
}