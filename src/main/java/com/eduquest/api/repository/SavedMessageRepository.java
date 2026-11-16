package com.eduquest.api.repository;

import com.eduquest.api.entity.SavedMessage;
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
public interface SavedMessageRepository extends JpaRepository<SavedMessage, Long> {
    Optional<SavedMessage> findByUserIdAndMessageId(Long userId, Long messageId);

    List<SavedMessage> findByUserIdOrderBySavedAtDesc(Long userId);

    long countByUserId(Long userId);

    @Query("SELECT s.message.id FROM SavedMessage s WHERE s.user.id = :userId AND s.message.id IN :messageIds")
    List<Long> findSavedIds(@Param("userId") Long userId, @Param("messageIds") Collection<Long> messageIds);

    @Modifying
    @Query("DELETE FROM SavedMessage s WHERE s.message.group.id = :groupId")
    void deleteByGroupId(@Param("groupId") Long groupId);

    @Modifying
    @Query("DELETE FROM SavedMessage s WHERE s.message.channel.id = :channelId")
    void deleteByChannelId(@Param("channelId") Long channelId);
}
