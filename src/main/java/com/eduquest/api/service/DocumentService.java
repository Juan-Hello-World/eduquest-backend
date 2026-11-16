package com.eduquest.api.service;

import com.eduquest.api.dto.request.DocumentRequestDTO;
import com.eduquest.api.dto.response.DocumentResponseDTO;
import com.eduquest.api.dto.response.PageResponseDTO;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface DocumentService {
    DocumentResponseDTO createDocument(DocumentRequestDTO request, String username);
    DocumentResponseDTO uploadDocumentToGroup(Long groupId, DocumentRequestDTO request, String username);
    PageResponseDTO<DocumentResponseDTO> getAllDocuments(Pageable pageable);
    PageResponseDTO<DocumentResponseDTO> getAllDocuments(String search, String tag, Pageable pageable);
    PageResponseDTO<DocumentResponseDTO> getAllDocuments(String search, String tag, String course,
                                                         String university, Pageable pageable);
    void saveDocument(Long documentId, String username);
    void unsaveDocument(Long documentId, String username);
    PageResponseDTO<DocumentResponseDTO> getSavedDocuments(String username, Pageable pageable);
    List<Long> getSavedDocumentIds(String username);
    void deleteDocument(Long documentId);
    DocumentResponseDTO updateDocument(Long documentId, DocumentRequestDTO request, String username);
    void deleteOwnDocument(Long documentId, String username);
    DocumentResponseDTO setFeatured(Long documentId, boolean featured);
    List<DocumentResponseDTO> getFeaturedDocuments();
    PageResponseDTO<DocumentResponseDTO> getPublicDocumentsByAuthor(Long authorId, Pageable pageable);
    PageResponseDTO<DocumentResponseDTO> getDocumentsByGroup(Long groupId, String username, Pageable pageable);
}