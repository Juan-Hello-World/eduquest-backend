package com.eduquest.api.repository;

import com.eduquest.api.entity.GroupChannel;
import com.eduquest.api.entity.GroupMessage;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface GroupMessageRepository extends JpaRepository<GroupMessage, Long> {
    List<GroupMessage> findTop50ByGroupIdOrderByIdDesc(Long groupId);

    List<GroupMessage> findTop50ByChannelIdAndThreadRootIsNullOrderByIdDesc(Long channelId);

    List<GroupMessage> findTop50ByChannelIdAndThreadRootIsNullAndIdLessThanOrderByIdDesc(Long channelId, Long beforeId);

    List<GroupMessage> findTop100ByChannelIdAndThreadRootIsNullAndIdGreaterThanOrderByIdAsc(Long channelId, Long afterId);

    List<GroupMessage> findByThreadRootIdOrderByIdAsc(Long threadRootId);

    Optional<GroupMessage> findByPollId(Long pollId);

    List<GroupMessage> findByChannelIdAndPinnedTrueOrderByPinnedAtDesc(Long channelId);

    @Query("SELECT m.threadRoot.id, COUNT(m) FROM GroupMessage m WHERE m.threadRoot.id IN :ids GROUP BY m.threadRoot.id")
    List<Object[]> countThreadReplies(@Param("ids") List<Long> ids);

    @Query("SELECT m FROM GroupMessage m WHERE m.group.id = :groupId AND (m.deleted IS NULL OR m.deleted = false) "
            + "AND LOWER(m.content) LIKE :pattern ESCAPE '\\' AND (:channelId IS NULL OR m.channel.id = :channelId) ORDER BY m.id DESC")
    List<GroupMessage> search(@Param("groupId") Long groupId, @Param("channelId") Long channelId,
                              @Param("pattern") String pattern, Pageable pageable);

    @Modifying
    @Query("UPDATE GroupMessage m SET m.channel = :channel WHERE m.group.id = :groupId AND m.channel IS NULL")
    int assignChannel(@Param("groupId") Long groupId, @Param("channel") GroupChannel channel);

    @Modifying
    @Query("UPDATE GroupMessage m SET m.replyTo = null, m.threadRoot = null, m.poll = null WHERE m.channel.id = :channelId")
    void clearLinksByChannelId(@Param("channelId") Long channelId);

    @Modifying
    @Query("UPDATE GroupMessage m SET m.replyTo = null WHERE m.replyTo.channel.id = :channelId")
    void clearRepliesToChannel(@Param("channelId") Long channelId);

    @Query("SELECT m.poll.id FROM GroupMessage m WHERE m.channel.id = :channelId AND m.poll IS NOT NULL")
    List<Long> findPollIdsByChannelId(@Param("channelId") Long channelId);

    @Modifying
    @Query("DELETE FROM GroupMessage m WHERE m.channel.id = :channelId")
    void deleteByChannelId(@Param("channelId") Long channelId);

    List<GroupMessage> findTop50ByGroupIdAndIdLessThanOrderByIdDesc(Long groupId, Long beforeId);

    List<GroupMessage> findTop100ByGroupIdAndIdGreaterThanOrderByIdAsc(Long groupId, Long afterId);

    long countByGroupIdAndCreatedAtAfterAndAuthorIdNot(Long groupId, LocalDateTime after, Long authorId);

    long countByGroupIdAndAuthorIdNot(Long groupId, Long authorId);

    @Query("SELECT COUNT(m) FROM GroupMessage m WHERE m.group.id = :groupId AND m.createdAt > :after "
            + "AND m.author.id <> :userId AND (m.deleted IS NULL OR m.deleted = false) AND LOWER(m.content) LIKE :mention ESCAPE '\\'")
    long countMentionsSince(@Param("groupId") Long groupId, @Param("after") LocalDateTime after,
                            @Param("userId") Long userId, @Param("mention") String mention);

    List<GroupMessage> findTop25ByChannelIdAndThreadRootIsNullAndIdLessThanEqualOrderByIdDesc(Long channelId, Long id);

    List<GroupMessage> findTop25ByChannelIdAndThreadRootIsNullAndIdGreaterThanOrderByIdAsc(Long channelId, Long id);

    Optional<GroupMessage> findTopByChannelIdOrderByIdDesc(Long channelId);

    Optional<GroupMessage> findTopByChannelIdAndAuthorIdOrderByIdDesc(Long channelId, Long authorId);

    @Query("SELECT m.channel.id, COUNT(m) FROM GroupMessage m WHERE m.group.id = :groupId AND m.channel IS NOT NULL "
            + "AND m.author.id <> :userId AND (m.deleted IS NULL OR m.deleted = false) "
            + "AND m.id > COALESCE((SELECT r.lastReadMessageId FROM ChannelRead r WHERE r.user.id = :userId AND r.channel.id = m.channel.id), 0) "
            + "GROUP BY m.channel.id")
    List<Object[]> countUnreadByChannel(@Param("groupId") Long groupId, @Param("userId") Long userId);

    @Query("SELECT m.channel.id, COUNT(m) FROM GroupMessage m WHERE m.group.id = :groupId AND m.channel IS NOT NULL "
            + "AND m.author.id <> :userId AND (m.deleted IS NULL OR m.deleted = false) AND LOWER(m.content) LIKE :mention ESCAPE '\\' "
            + "AND m.id > COALESCE((SELECT r.lastReadMessageId FROM ChannelRead r WHERE r.user.id = :userId AND r.channel.id = m.channel.id), 0) "
            + "GROUP BY m.channel.id")
    List<Object[]> countMentionsByChannel(@Param("groupId") Long groupId, @Param("userId") Long userId,
                                          @Param("mention") String mention);

    @Query("SELECT m FROM GroupMessage m WHERE m.channel.id = :channelId AND m.threadRoot IS NULL "
            + "AND (m.deleted IS NULL OR m.deleted = false) AND m.id > :afterId ORDER BY m.id ASC")
    List<GroupMessage> findForSummary(@Param("channelId") Long channelId, @Param("afterId") Long afterId, Pageable pageable);

    @Query("SELECT m FROM GroupMessage m WHERE m.channel.id = :channelId AND m.threadRoot IS NULL "
            + "AND (m.deleted IS NULL OR m.deleted = false) ORDER BY m.id DESC")
    List<GroupMessage> findLatestForSummary(@Param("channelId") Long channelId, Pageable pageable);

    @Modifying
    @Query("UPDATE GroupMessage m SET m.replyTo = null, m.threadRoot = null, m.poll = null WHERE m.group.id = :groupId")
    void clearRepliesByGroupId(@Param("groupId") Long groupId);

    @Modifying
    @Query("DELETE FROM GroupMessage m WHERE m.group.id = :groupId")
    void deleteByGroupId(@Param("groupId") Long groupId);
}
