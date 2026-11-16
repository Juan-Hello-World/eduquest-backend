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

    List<StudySession> findByEndsAtIsNull();

    @Query("SELECT s FROM StudySession s JOIN s.privateGroup g JOIN g.members m "
            + "WHERE m.id = :userId AND s.scheduledAt >= :from AND s.scheduledAt < :to ORDER BY s.scheduledAt")
    List<StudySession> findForMemberBetween(@Param("userId") Long userId,
                                           @Param("from") java.time.LocalDateTime from,
                                           @Param("to") java.time.LocalDateTime to);

    Optional<StudySession> findByIdAndPrivateGroupId(Long id, Long groupId);

    long countByCreatedById(Long userId);

    long countByCreatedAtAfter(LocalDateTime after);

    @Query("SELECT s.createdBy.id, COUNT(s) FROM StudySession s WHERE s.scheduledAt >= :since AND EXISTS ("
            + "SELECT a.id FROM SessionAttendance a WHERE a.session = s AND a.checkedInAt IS NOT NULL"
            + " AND a.user <> s.createdBy) GROUP BY s.createdBy.id")
    List<Object[]> countAttendedByCreatorSince(@Param("since") LocalDateTime since);

    long countByPrivateGroupIdAndCreatedAtAfterAndCreatedByIdNot(Long groupId, LocalDateTime after, Long userId);
}