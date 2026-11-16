package com.eduquest.api.repository;

import com.eduquest.api.entity.MessageReaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface MessageReactionRepository extends JpaRepository<MessageReaction, Long> {

    Optional<MessageReaction> findByMessageIdAndUserIdAndEmoji(Long messageId, Long userId, String emoji);

    long countByMessageIdAndUserId(Long messageId, Long userId);

    List<MessageReaction> findByMessageIdIn(Collection<Long> messageIds);

    @Modifying
    @Query("DELETE FROM MessageReaction r WHERE r.message.group.id = :groupId")
    void deleteByGroupId(@Param("groupId") Long groupId);

    @Modifying
    @Query("DELETE FROM MessageReaction r WHERE r.message.channel.id = :channelId")
    void deleteByChannelId(@Param("channelId") Long channelId);
}
