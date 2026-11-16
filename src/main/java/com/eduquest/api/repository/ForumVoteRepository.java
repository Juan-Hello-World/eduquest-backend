package com.eduquest.api.repository;

import com.eduquest.api.entity.ForumVote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ForumVoteRepository extends JpaRepository<ForumVote, Long> {

    Optional<ForumVote> findByAnswerIdAndUserId(Long answerId, Long userId);

    @Query("SELECT v.answer.id, COALESCE(SUM(v.value), 0) FROM ForumVote v WHERE v.answer.question.id = :questionId GROUP BY v.answer.id")
    List<Object[]> sumByQuestion(@Param("questionId") Long questionId);

    @Query("SELECT v FROM ForumVote v WHERE v.answer.question.id = :questionId AND v.user.id = :userId")
    List<ForumVote> findByQuestionAndUser(@Param("questionId") Long questionId, @Param("userId") Long userId);

    @Query("SELECT COALESCE(SUM(v.value), 0) FROM ForumVote v WHERE v.answer.id = :answerId")
    long scoreOf(@Param("answerId") Long answerId);

    @Modifying
    @Query("DELETE FROM ForumVote v WHERE v.answer.id = :answerId")
    void deleteByAnswerId(@Param("answerId") Long answerId);

    @Modifying
    @Query("DELETE FROM ForumVote v WHERE v.answer.question.id = :questionId")
    void deleteByQuestionId(@Param("questionId") Long questionId);
}
