package com.eduquest.api.repository;

import com.eduquest.api.entity.ForumQuestion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ForumQuestionRepository extends JpaRepository<ForumQuestion, Long> {

    @Query("SELECT q FROM ForumQuestion q WHERE "
            + "(LOWER(q.title) LIKE :pattern OR LOWER(q.body) LIKE :pattern) "
            + "AND (:course = '' OR LOWER(q.course) = :course) "
            + "AND (:filter = 'all' "
            + "  OR (:filter = 'solved' AND q.acceptedAnswerId IS NOT NULL) "
            + "  OR (:filter = 'unanswered' AND NOT EXISTS (SELECT a.id FROM ForumAnswer a WHERE a.question = q))) "
            + "ORDER BY q.createdAt DESC")
    Page<ForumQuestion> search(@Param("pattern") String pattern,
                               @Param("course") String course,
                               @Param("filter") String filter,
                               Pageable pageable);
}
