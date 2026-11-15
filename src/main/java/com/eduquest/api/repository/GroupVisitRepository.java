package com.eduquest.api.repository;

import com.eduquest.api.entity.GroupVisit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GroupVisitRepository extends JpaRepository<GroupVisit, Long> {

    Optional<GroupVisit> findByUserIdAndGroupId(Long userId, Long groupId);

    List<GroupVisit> findByUserId(Long userId);

    boolean existsByUserIdAndGroupId(Long userId, Long groupId);
}
