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

    String CONFIRMED = "(a.confirmed IS NULL OR a.confirmed = true)";
    String SESSION_END = "a.session.endsAt";

    @Query("SELECT COUNT(a) FROM SessionAttendance a WHERE a.user.id = :userId AND " + CONFIRMED)
    long countConfirmedByUserId(@Param("userId") Long userId);

    long countByUserIdAndCheckedInAtIsNotNull(Long userId);

    @Query("SELECT COUNT(a) FROM SessionAttendance a WHERE a.user.id = :userId AND " + CONFIRMED
            + " AND a.checkedInAt IS NULL AND " + SESSION_END + " < :cutoff")
    long countNoShowsByUserId(@Param("userId") Long userId, @Param("cutoff") LocalDateTime cutoff);

    @Query("SELECT a.user.id, COUNT(a) FROM SessionAttendance a WHERE a.createdAt >= :since AND " + CONFIRMED
            + " GROUP BY a.user.id")
    List<Object[]> countConfirmedByUserSince(@Param("since") LocalDateTime since);

    @Query("SELECT a.user.id, COUNT(a) FROM SessionAttendance a WHERE a.checkedInAt >= :since GROUP BY a.user.id")
    List<Object[]> countCheckInsByUserSince(@Param("since") LocalDateTime since);

    @Query("SELECT a.user.id, COUNT(a) FROM SessionAttendance a WHERE " + CONFIRMED + " AND a.checkedInAt IS NULL"
            + " AND " + SESSION_END + " >= :since AND " + SESSION_END + " < :cutoff GROUP BY a.user.id")
    List<Object[]> countNoShowsByUserSince(@Param("since") LocalDateTime since, @Param("cutoff") LocalDateTime cutoff);
}
