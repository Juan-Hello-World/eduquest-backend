package com.eduquest.api.controller;

import com.eduquest.api.dto.request.ReviewRequestDTO;
import com.eduquest.api.dto.response.PageResponseDTO;
import com.eduquest.api.dto.response.ReviewResponseDTO;
import com.eduquest.api.security.UserPrincipal;
import com.eduquest.api.service.ReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Reseñas", description = "Reseñas de documentos. Borrar reseña ajena solo ADMIN/MANAGER.")
public class ReviewController {

    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @PostMapping("/reviews")
    @Operation(summary = "Crear reseña", description = "El autor queda fijado como el usuario autenticado.")
    public ResponseEntity<ReviewResponseDTO> createReview(
            @Valid @RequestBody ReviewRequestDTO request,
            @AuthenticationPrincipal UserPrincipal principal) {
        ReviewResponseDTO response = reviewService.createReview(request, principal.getUsername());
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/documents/{documentId}/reviews")
    @Operation(summary = "Listar reseñas de un documento", description = "Devuelve reseñas paginadas.")
    public ResponseEntity<PageResponseDTO<ReviewResponseDTO>> getReviewsByDocument(
            @PathVariable Long documentId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100));
        return ResponseEntity.ok(reviewService.getReviewsByDocument(documentId, pageable));
    }

    @GetMapping("/reviews/{id}")
    @Operation(summary = "Obtener reseña por id")
    public ResponseEntity<ReviewResponseDTO> getReview(@PathVariable Long id) {
        return ResponseEntity.ok(reviewService.getReview(id));
    }

    @DeleteMapping("/reviews/{id}")
    @Operation(summary = "Eliminar reseña", description = "El dueño de la reseña, o un ADMIN/MANAGER, puede eliminarla.")
    public ResponseEntity<Void> deleteReview(@PathVariable Long id,
                                             @AuthenticationPrincipal UserPrincipal principal) {
        reviewService.deleteReview(id, principal.getUsername());
        return ResponseEntity.noContent().build();
    }
}