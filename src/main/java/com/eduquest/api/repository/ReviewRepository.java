package com.eduquest.api.repository;

import com.eduquest.api.entity.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {
    Page<Review> findByDocumentId(Long documentId, Pageable pageable);
    boolean existsByAuthorIdAndDocumentId(Long authorId, Long documentId);
}