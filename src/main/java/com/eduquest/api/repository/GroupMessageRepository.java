package com.eduquest.api.repository;

import com.eduquest.api.entity.GroupMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface GroupMessageRepository extends JpaRepository<GroupMessage, Long> {
    List<GroupMessage> findTop50ByGroupIdOrderByIdDesc(Long groupId);

    List<GroupMessage> findTop50ByGroupIdAndIdLessThanOrderByIdDesc(Long groupId, Long beforeId);

    List<GroupMessage> findTop100ByGroupIdAndIdGreaterThanOrderByIdAsc(Long groupId, Long afterId);

    long countByGroupIdAndCreatedAtAfterAndAuthorIdNot(Long groupId, LocalDateTime after, Long authorId);

    long countByGroupIdAndAuthorIdNot(Long groupId, Long authorId);
}
