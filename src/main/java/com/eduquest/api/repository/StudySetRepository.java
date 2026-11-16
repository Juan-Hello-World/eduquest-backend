package com.eduquest.api.repository;

import com.eduquest.api.entity.StudySet;
import com.eduquest.api.entity.StudySetType;
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
public interface StudySetRepository extends JpaRepository<StudySet, Long> {
    Optional<StudySet> findByDocumentIdAndType(Long documentId, StudySetType type);

    @Modifying
    @Query("DELETE FROM StudySet s WHERE s.document.id = :documentId")
    void deleteByDocumentId(@Param("documentId") Long documentId);
}
