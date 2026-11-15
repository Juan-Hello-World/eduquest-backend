package com.eduquest.api.repository;

import com.eduquest.api.entity.SessionAttendance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface SessionAttendanceRepository extends JpaRepository<SessionAttendance, Long> {
    long countByUserId(Long userId);

    @Query("SELECT a.user.id, COUNT(a) FROM SessionAttendance a WHERE a.createdAt >= :since GROUP BY a.user.id")
    List<Object[]> countByUserSince(@Param("since") LocalDateTime since);
}
