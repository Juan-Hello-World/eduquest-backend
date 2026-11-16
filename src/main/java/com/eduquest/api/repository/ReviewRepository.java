package com.eduquest.api.repository;

import com.eduquest.api.entity.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {
    Page<Review> findByDocumentId(Long documentId, Pageable pageable);
    boolean existsByAuthorIdAndDocumentId(Long authorId, Long documentId);

    long countByAuthorId(Long authorId);

    void deleteByDocumentId(Long documentId);

    @Query("SELECT r.author.id, COUNT(r) FROM Review r WHERE r.createdAt >= :since GROUP BY r.author.id")
    List<Object[]> countByAuthorSince(@Param("since") LocalDateTime since);

    @Query("SELECT r.document.author.id, COUNT(r) FROM Review r WHERE r.rating >= 4"
            + " AND r.author.id <> r.document.author.id AND r.createdAt >= :since GROUP BY r.document.author.id")
    List<Object[]> countWellRatedByDocumentAuthorSince(@Param("since") LocalDateTime since);
}