package com.eduquest.api.repository;

import com.eduquest.api.entity.Document;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DocumentRepository extends JpaRepository<Document, Long> {
List<Document> findByTitleContainingIgnoreCase(String title);

@Query("SELECT DISTINCT d FROM Document d LEFT JOIN d.tags t " +
        "WHERE d.group IS NULL " +
        "AND (:searchPattern IS NULL OR LOWER(d.title) LIKE :searchPattern " +
        "     OR LOWER(COALESCE(d.description, '')) LIKE :searchPattern) " +
        "AND (:tag IS NULL OR LOWER(t.name) = :tag)")
Page<Document> searchPublicDocuments(@Param("searchPattern") String searchPattern,
                                     @Param("tag") String tag,
                                     Pageable pageable);

Page<Document> findByGroupId(Long groupId, Pageable pageable);
Page<Document> findByGroupIsNull(Pageable pageable);
}