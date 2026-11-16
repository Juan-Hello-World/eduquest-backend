package com.eduquest.api.repository;

import com.eduquest.api.entity.ChannelRead;
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
public interface ChannelReadRepository extends JpaRepository<ChannelRead, Long> {
    Optional<ChannelRead> findByUserIdAndChannelId(Long userId, Long channelId);

    List<ChannelRead> findByUserIdAndChannelGroupId(Long userId, Long groupId);

    @Modifying
    @Query("DELETE FROM ChannelRead r WHERE r.channel.id = :channelId")
    void deleteByChannelId(@Param("channelId") Long channelId);

    @Modifying
    @Query("DELETE FROM ChannelRead r WHERE r.channel.group.id = :groupId")
    void deleteByGroupId(@Param("groupId") Long groupId);
}
