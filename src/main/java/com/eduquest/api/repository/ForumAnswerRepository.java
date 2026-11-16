package com.eduquest.api.repository;

import com.eduquest.api.entity.ForumAnswer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ForumAnswerRepository extends JpaRepository<ForumAnswer, Long> {

    List<ForumAnswer> findByQuestionIdOrderByCreatedAtAsc(Long questionId);

    long countByQuestionId(Long questionId);

    @Query("SELECT a.question.id, COUNT(a) FROM ForumAnswer a WHERE a.question.id IN :ids GROUP BY a.question.id")
    List<Object[]> countByQuestionIds(@Param("ids") List<Long> ids);

    @Query("SELECT a.author.id, COUNT(a) FROM ForumAnswer a WHERE a.id = a.question.acceptedAnswerId"
            + " AND a.author.id <> a.question.author.id AND a.createdAt >= :since GROUP BY a.author.id")
    List<Object[]> countAcceptedByAuthorSince(@Param("since") LocalDateTime since);

    @Modifying
    @Query("DELETE FROM ForumAnswer a WHERE a.question.id = :questionId")
    void deleteByQuestionId(@Param("questionId") Long questionId);
}
