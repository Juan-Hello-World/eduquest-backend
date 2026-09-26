package com.eduquest.api.service.impl;

import com.eduquest.api.dto.request.ReviewRequestDTO;
import com.eduquest.api.dto.response.PageResponseDTO;
import com.eduquest.api.dto.response.ReviewResponseDTO;
import com.eduquest.api.entity.Document;
import com.eduquest.api.entity.Review;
import com.eduquest.api.entity.Role;
import com.eduquest.api.entity.RoleName;
import com.eduquest.api.entity.User;
import com.eduquest.api.exception.DuplicateResourceException;
import com.eduquest.api.exception.ForbiddenException;
import com.eduquest.api.exception.ResourceNotFoundException;
import com.eduquest.api.repository.DocumentRepository;
import com.eduquest.api.repository.ReviewRepository;
import com.eduquest.api.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ReviewServiceImplTest {

    @Mock ReviewRepository reviewRepository;
    @Mock DocumentRepository documentRepository;
    @Mock UserRepository userRepository;

    @InjectMocks ReviewServiceImpl reviewService;

    private User alice;
    private Document document;
    private Review review;

    @BeforeEach
    void setUp() {
        alice = new User();
        alice.setId(1L);
        alice.setUsername("alice");
        alice.setEmail("alice@utec.edu.pe");

        document = new Document();
        document.setId(1L);
        document.setTitle("Apuntes");
        document.setFileUrl("https://x/a.pdf");
        document.setAuthor(alice);

        review = new Review();
        review.setId(1L);
        review.setRating(5);
        review.setComment("Excelente");
        review.setAuthor(alice);
        review.setDocument(document);
    }

    @Test
    void createReview_withoutExistingReview_returnsCreatedReview() {
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(alice));
        when(documentRepository.findById(1L)).thenReturn(Optional.of(document));
        when(reviewRepository.existsByAuthorIdAndDocumentId(1L, 1L)).thenReturn(false);
        when(reviewRepository.save(any(Review.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ReviewRequestDTO request = new ReviewRequestDTO();
        request.setDocumentId(1L);
        request.setRating(5);
        request.setComment("Excelente");

        ReviewResponseDTO response = reviewService.createReview(request, "alice");

        assertThat(response.getRating()).isEqualTo(5);
        assertThat(response.getAuthorUsername()).isEqualTo("alice");
    }

    @Test
    void createReview_withUnknownDocument_throwsNotFound() {
        when(documentRepository.findById(99L)).thenReturn(Optional.empty());

        ReviewRequestDTO request = new ReviewRequestDTO();
        request.setDocumentId(99L);
        request.setRating(3);

        assertThatThrownBy(() -> reviewService.createReview(request, "alice"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void createReview_whenAlreadyReviewed_throwsConflict() {
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(alice));
        when(documentRepository.findById(1L)).thenReturn(Optional.of(document));
        when(reviewRepository.existsByAuthorIdAndDocumentId(1L, 1L)).thenReturn(true);

        ReviewRequestDTO request = new ReviewRequestDTO();
        request.setDocumentId(1L);
        request.setRating(4);

        assertThatThrownBy(() -> reviewService.createReview(request, "alice"))
                .isInstanceOf(DuplicateResourceException.class);
    }

    @Test
    void getReviewsByDocument_returnsPaginatedReviews() {
        when(documentRepository.existsById(1L)).thenReturn(true);
        Pageable pageable = PageRequest.of(0, 10);
        when(reviewRepository.findByDocumentId(1L, pageable))
                .thenReturn(new PageImpl<>(List.of(review), pageable, 1));

        PageResponseDTO<ReviewResponseDTO> page = reviewService.getReviewsByDocument(1L, pageable);

        assertThat(page.content()).hasSize(1);
        assertThat(page.content().get(0).getDocumentTitle()).isEqualTo("Apuntes");
    }

    @Test
    void getReviewsByDocument_withUnknownDocument_throwsNotFound() {
        when(documentRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> reviewService.getReviewsByDocument(99L, PageRequest.of(0, 10)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getReview_returnsMappedReview() {
        when(reviewRepository.findById(1L)).thenReturn(Optional.of(review));

        ReviewResponseDTO response = reviewService.getReview(1L);

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getComment()).isEqualTo("Excelente");
    }

    @Test
    void getReview_withUnknownId_throwsNotFound() {
        when(reviewRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reviewService.getReview(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void deleteReview_asOwner_removesReview() {
        when(reviewRepository.findById(1L)).thenReturn(Optional.of(review));
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(alice));

        reviewService.deleteReview(1L, "alice");

        verify(reviewRepository).delete(review);
    }

    @Test
    void deleteReview_asAdmin_removesOthersReview() {
        User admin = new User();
        admin.setId(2L);
        admin.setUsername("admin");
        admin.setRoles(Set.of(new Role(2L, RoleName.ROLE_ADMIN)));

        when(reviewRepository.findById(1L)).thenReturn(Optional.of(review));
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(admin));

        reviewService.deleteReview(1L, "admin");

        verify(reviewRepository).delete(review);
    }

    @Test
    void deleteReview_asOtherUser_throwsForbidden() {
        User bob = new User();
        bob.setId(3L);
        bob.setUsername("bob");
        bob.setRoles(Set.of(new Role(1L, RoleName.ROLE_USER)));

        when(reviewRepository.findById(1L)).thenReturn(Optional.of(review));
        when(userRepository.findByUsername("bob")).thenReturn(Optional.of(bob));

        assertThatThrownBy(() -> reviewService.deleteReview(1L, "bob"))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void deleteReview_withUnknownReview_throwsNotFound() {
        when(reviewRepository.findById(anyLong())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reviewService.deleteReview(99L, "alice"))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}