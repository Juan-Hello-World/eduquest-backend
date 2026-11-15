package com.eduquest.api.repository;

import com.eduquest.api.entity.DirectMessage;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface DirectMessageRepository extends JpaRepository<DirectMessage, Long> {

    String BETWEEN = "((m.sender.id = :me AND m.recipient.id = :other) OR (m.sender.id = :other AND m.recipient.id = :me))";

    @Query("SELECT m FROM DirectMessage m WHERE " + BETWEEN + " ORDER BY m.id DESC")
    List<DirectMessage> findLatest(@Param("me") Long me, @Param("other") Long other, Pageable pageable);

    @Query("SELECT m FROM DirectMessage m WHERE " + BETWEEN + " AND m.id < :before ORDER BY m.id DESC")
    List<DirectMessage> findBefore(@Param("me") Long me, @Param("other") Long other,
                                   @Param("before") Long before, Pageable pageable);

    @Query("SELECT m FROM DirectMessage m WHERE " + BETWEEN + " AND m.id > :after ORDER BY m.id ASC")
    List<DirectMessage> findAfter(@Param("me") Long me, @Param("other") Long other,
                                  @Param("after") Long after, Pageable pageable);

    @Query("SELECT DISTINCT CASE WHEN m.sender.id = :me THEN m.recipient.id ELSE m.sender.id END "
            + "FROM DirectMessage m WHERE m.sender.id = :me OR m.recipient.id = :me")
    List<Long> findPartnerIds(@Param("me") Long me);

    long countBySenderIdAndRecipientIdAndReadAtIsNull(Long senderId, Long recipientId);

    long countByRecipientIdAndReadAtIsNull(Long recipientId);

    @Modifying
    @Query("UPDATE DirectMessage m SET m.readAt = :now WHERE m.sender.id = :other AND m.recipient.id = :me AND m.readAt IS NULL")
    int markRead(@Param("me") Long me, @Param("other") Long other, @Param("now") LocalDateTime now);
}
