package com.eduquest.api.service.impl;

import com.eduquest.api.dto.request.ReviewRequestDTO;
import com.eduquest.api.dto.response.PageResponseDTO;
import com.eduquest.api.dto.response.ReviewResponseDTO;
import com.eduquest.api.entity.Document;
import com.eduquest.api.entity.Review;
import com.eduquest.api.entity.RoleName;
import com.eduquest.api.entity.User;
import com.eduquest.api.exception.DuplicateResourceException;
import com.eduquest.api.exception.ForbiddenException;
import com.eduquest.api.exception.ResourceNotFoundException;
import com.eduquest.api.repository.DocumentRepository;
import com.eduquest.api.repository.ReviewRepository;
import com.eduquest.api.repository.UserRepository;
import com.eduquest.api.service.ReviewService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ReviewServiceImpl implements ReviewService {

    private final ReviewRepository reviewRepository;
    private final DocumentRepository documentRepository;
    private final UserRepository userRepository;

    public ReviewServiceImpl(ReviewRepository reviewRepository, DocumentRepository documentRepository,
                             UserRepository userRepository) {
        this.reviewRepository = reviewRepository;
        this.documentRepository = documentRepository;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public ReviewResponseDTO createReview(ReviewRequestDTO request, String username) {
        User author = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        Document document = documentRepository.findById(request.getDocumentId())
                .orElseThrow(() -> new ResourceNotFoundException("Documento no encontrado"));

        if (reviewRepository.existsByAuthorIdAndDocumentId(author.getId(), document.getId())) {
            throw new DuplicateResourceException("Ya has reseñado este documento");
        }

        Review review = new Review();
        review.setRating(request.getRating());
        review.setComment(request.getComment());
        review.setAuthor(author);
        review.setDocument(document);

        return mapToResponseDTO(reviewRepository.save(review));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponseDTO<ReviewResponseDTO> getReviewsByDocument(Long documentId, Pageable pageable) {
        if (!documentRepository.existsById(documentId)) {
            throw new ResourceNotFoundException("Documento no encontrado");
        }
        Page<Review> reviewPage = reviewRepository.findByDocumentId(documentId, pageable);
        List<ReviewResponseDTO> content = reviewPage.getContent().stream()
                .map(this::mapToResponseDTO)
                .collect(Collectors.toList());
        return PageResponseDTO.of(content, reviewPage.getNumber(), reviewPage.getSize(), reviewPage.getTotalElements());
    }

    @Override
    @Transactional(readOnly = true)
    public ReviewResponseDTO getReview(Long id) {
        return mapToResponseDTO(findReview(id));
    }

    @Override
    @Transactional
    public void deleteReview(Long id, String username) {
        Review review = findReview(id);

        User requester = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        boolean isOwner = review.getAuthor().getUsername().equals(username);
        boolean isAdminOrManager = requester.getRoles().stream()
                .anyMatch(role -> role.getName() == RoleName.ROLE_ADMIN
                        || role.getName() == RoleName.ROLE_MANAGER);

        if (!isOwner && !isAdminOrManager) {
            throw new ForbiddenException("No puedes eliminar una reseña que no te pertenece");
        }

        reviewRepository.delete(review);
    }

    private Review findReview(Long id) {
        return reviewRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Reseña no encontrada"));
    }

    private ReviewResponseDTO mapToResponseDTO(Review review) {
        ReviewResponseDTO dto = new ReviewResponseDTO();
        dto.setId(review.getId());
        dto.setRating(review.getRating());
        dto.setComment(review.getComment());
        dto.setCreatedAt(review.getCreatedAt());
        if (review.getAuthor() != null) {
            dto.setAuthorUsername(review.getAuthor().getUsername());
        }
        if (review.getDocument() != null) {
            dto.setDocumentTitle(review.getDocument().getTitle());
        }
        return dto;
    }
}