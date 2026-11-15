package com.eduquest.api.repository;

import com.eduquest.api.entity.StudySession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface StudySessionRepository extends JpaRepository<StudySession, Long> {
    List<StudySession> findByPrivateGroupId(Long groupId);

    Optional<StudySession> findByIdAndPrivateGroupId(Long id, Long groupId);

    long countByCreatedById(Long userId);

    @Query("SELECT s.createdBy.id, COUNT(s) FROM StudySession s WHERE s.createdAt >= :since GROUP BY s.createdBy.id")
    List<Object[]> countByCreatorSince(@Param("since") LocalDateTime since);

    long countByPrivateGroupIdAndCreatedAtAfterAndCreatedByIdNot(Long groupId, LocalDateTime after, Long userId);
}