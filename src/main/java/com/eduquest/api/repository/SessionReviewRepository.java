package com.eduquest.api.repository;

import com.eduquest.api.entity.SessionReview;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface SessionReviewRepository extends JpaRepository<SessionReview, Long> {
    List<SessionReview> findBySessionIdOrderByCreatedAtDesc(Long sessionId);

    long countByAuthorId(Long authorId);

    long countBySessionCreatedById(Long userId);

    @Query("SELECT AVG(r.rating) FROM SessionReview r WHERE r.session.createdBy.id = :userId")
    Double averageRatingForHost(@Param("userId") Long userId);

    @Query("SELECT r.author.id, COUNT(r) FROM SessionReview r WHERE r.createdAt >= :since GROUP BY r.author.id")
    List<Object[]> countByAuthorSince(@Param("since") LocalDateTime since);
}
