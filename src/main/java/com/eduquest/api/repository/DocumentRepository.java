package com.eduquest.api.repository;

import com.eduquest.api.entity.Document;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
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
        "AND (:tag IS NULL OR LOWER(t.name) = :tag) " +
        "AND (:coursePattern IS NULL OR LOWER(COALESCE(d.course, '')) LIKE :coursePattern) " +
        "AND (:universityPattern IS NULL OR LOWER(COALESCE(d.university, '')) LIKE :universityPattern)")
Page<Document> searchPublicDocuments(@Param("searchPattern") String searchPattern,
                                     @Param("tag") String tag,
                                     @Param("coursePattern") String coursePattern,
                                     @Param("universityPattern") String universityPattern,
                                     Pageable pageable);

@Query(value = "SELECT d FROM User u JOIN u.savedDocuments d WHERE u.id = :userId",
        countQuery = "SELECT COUNT(d) FROM User u JOIN u.savedDocuments d WHERE u.id = :userId")
Page<Document> findSavedByUserId(@Param("userId") Long userId, Pageable pageable);

@Query("SELECT d.id FROM User u JOIN u.savedDocuments d WHERE u.id = :userId")
List<Long> findSavedIdsByUserId(@Param("userId") Long userId);

@Modifying
@Query(value = "DELETE FROM saved_documents WHERE document_id = :documentId", nativeQuery = true)
void removeFromSavedLists(@Param("documentId") Long documentId);

long countByAuthorId(Long authorId);

@Query("SELECT d.author.id, COUNT(d) FROM Document d WHERE d.uploadedAt >= :since GROUP BY d.author.id")
List<Object[]> countByAuthorSince(@Param("since") java.time.LocalDateTime since);

Page<Document> findByGroupId(Long groupId, Pageable pageable);

List<Document> findAllByGroupId(Long groupId);

List<Document> findTop6ByFeaturedTrueAndGroupIsNullOrderByUploadedAtDesc();

long countByUploadedAtAfter(java.time.LocalDateTime after);

long countByGroupIdAndUploadedAtAfterAndAuthorIdNot(Long groupId, java.time.LocalDateTime after, Long authorId);
Page<Document> findByGroupIsNull(Pageable pageable);

Page<Document> findByAuthorIdAndGroupIsNull(Long authorId, Pageable pageable);
}