package com.eduquest.api.repository;

import com.eduquest.api.entity.GroupVisibility;
import com.eduquest.api.entity.PrivateGroup;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PrivateGroupRepository extends JpaRepository<PrivateGroup, Long> {

    boolean existsByIdAndMembersId(Long groupId, Long userId);

    boolean existsByCode(String code);

    Optional<PrivateGroup> findByCodeIgnoreCase(String code);

    List<PrivateGroup> findByCodeIsNull();

    @Query(value = """
            select g from PrivateGroup g
            where (g.visibility is null or g.visibility = :visibility
                   or exists (select m from g.members m where m.id = :userId))
              and lower(g.name) like :namePattern
            order by g.id desc
            """,
            countQuery = """
            select count(g) from PrivateGroup g
            where (g.visibility is null or g.visibility = :visibility
                   or exists (select m from g.members m where m.id = :userId))
              and lower(g.name) like :namePattern
            """)
    Page<PrivateGroup> findVisibleFor(@Param("userId") Long userId,
                                      @Param("visibility") GroupVisibility visibility,
                                      @Param("namePattern") String namePattern,
                                      Pageable pageable);

    List<PrivateGroup> findByMembersId(Long userId);

    @Query("SELECT g FROM PrivateGroup g WHERE LOWER(g.name) LIKE :pattern "
            + "AND (g.visibility = com.eduquest.api.entity.GroupVisibility.PUBLIC "
            + "OR EXISTS (SELECT m FROM g.members m WHERE m.id = :userId)) ORDER BY g.lastActivityAt DESC NULLS LAST")
    List<PrivateGroup> searchForUser(@Param("pattern") String pattern, @Param("userId") Long userId, Pageable pageable);

    @Query("SELECT m.id FROM PrivateGroup g JOIN g.members m WHERE g.id = :groupId")
    List<Long> findMemberIds(@Param("groupId") Long groupId);
}
