package com.eduquest.api.repository;

import com.eduquest.api.entity.RoomMessage;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface RoomMessageRepository extends JpaRepository<RoomMessage, Long> {
    List<RoomMessage> findTop50ByRoomIdOrderByIdDesc(Long roomId);

    List<RoomMessage> findTop50ByRoomIdAndIdLessThanOrderByIdDesc(Long roomId, Long beforeId);

    List<RoomMessage> findTop100ByRoomIdAndIdGreaterThanOrderByIdAsc(Long roomId, Long afterId);

    Optional<RoomMessage> findTopByRoomIdOrderByIdDesc(Long roomId);

    long countByRoomIdAndCreatedAtAfterAndAuthorIdNot(Long roomId, LocalDateTime after, Long authorId);

    long countByRoomIdAndAuthorIdNot(Long roomId, Long authorId);

    @Modifying
    @Query("UPDATE RoomMessage m SET m.replyTo = null WHERE m.room.id = :roomId")
    void clearRepliesByRoomId(@Param("roomId") Long roomId);

    @Modifying
    @Query("DELETE FROM RoomMessage m WHERE m.room.id = :roomId")
    void deleteByRoomId(@Param("roomId") Long roomId);
}
