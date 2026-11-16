package com.eduquest.api.repository;

import com.eduquest.api.entity.GroupVisit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GroupVisitRepository extends JpaRepository<GroupVisit, Long> {

    Optional<GroupVisit> findByUserIdAndGroupId(Long userId, Long groupId);

    List<GroupVisit> findByUserId(Long userId);

    @Query("SELECT v.user.id FROM GroupVisit v WHERE v.group.id = :groupId AND v.muted = true")
    List<Long> findMutedUserIds(@Param("groupId") Long groupId);

    boolean existsByUserIdAndGroupId(Long userId, Long groupId);

    @Modifying
    @Query("DELETE FROM GroupVisit v WHERE v.group.id = :groupId")
    void deleteByGroupId(@Param("groupId") Long groupId);

    @Modifying
    @Query("DELETE FROM GroupVisit v WHERE v.group.id = :groupId AND v.user.id = :userId")
    void deleteByGroupIdAndUserId(@Param("groupId") Long groupId, @Param("userId") Long userId);
}
