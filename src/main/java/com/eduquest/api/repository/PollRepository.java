package com.eduquest.api.repository;

import com.eduquest.api.entity.Poll;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PollRepository extends JpaRepository<Poll, Long> {

    @Modifying
    @Query("DELETE FROM PollVote v WHERE v.poll.group.id = :groupId")
    void deleteVotesByGroupId(@Param("groupId") Long groupId);

    @Modifying
    @Query("DELETE FROM PollOption o WHERE o.poll.group.id = :groupId")
    void deleteOptionsByGroupId(@Param("groupId") Long groupId);

    @Modifying
    @Query("DELETE FROM Poll p WHERE p.group.id = :groupId")
    void deleteByGroupId(@Param("groupId") Long groupId);

    @Modifying
    @Query("DELETE FROM PollVote v WHERE v.poll.id IN :pollIds")
    void deleteVotesByPollIds(@Param("pollIds") List<Long> pollIds);

    @Modifying
    @Query("DELETE FROM PollOption o WHERE o.poll.id IN :pollIds")
    void deleteOptionsByPollIds(@Param("pollIds") List<Long> pollIds);

    @Modifying
    @Query("DELETE FROM Poll p WHERE p.id IN :pollIds")
    void deleteByIds(@Param("pollIds") List<Long> pollIds);
}
