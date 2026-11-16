package com.eduquest.api.repository;

import com.eduquest.api.entity.GroupChannel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GroupChannelRepository extends JpaRepository<GroupChannel, Long> {

    List<GroupChannel> findByGroupIdOrderByPositionAscIdAsc(Long groupId);

    long countByGroupId(Long groupId);

    @Modifying
    @Query("DELETE FROM GroupChannel c WHERE c.group.id = :groupId")
    void deleteByGroupId(@Param("groupId") Long groupId);
}
