package com.eduquest.api.repository;

import com.eduquest.api.entity.PinnedChat;
import com.eduquest.api.entity.PinnedChatKind;
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
public interface PinnedChatRepository extends JpaRepository<PinnedChat, Long> {
    List<PinnedChat> findByUserIdOrderByPinnedAtAsc(Long userId);

    Optional<PinnedChat> findByUserIdAndKindAndTargetId(Long userId, PinnedChatKind kind, Long targetId);

    long countByUserId(Long userId);
}
