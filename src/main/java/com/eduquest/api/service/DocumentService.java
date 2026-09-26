package com.eduquest.api.service;

import com.eduquest.api.dto.request.DocumentRequestDTO;
import com.eduquest.api.dto.response.DocumentResponseDTO;
import com.eduquest.api.dto.response.PageResponseDTO;
import org.springframework.data.domain.Pageable;

public interface DocumentService {
    DocumentResponseDTO createDocument(DocumentRequestDTO request, String username);
    DocumentResponseDTO uploadDocumentToGroup(Long groupId, DocumentRequestDTO request, String username);
    PageResponseDTO<DocumentResponseDTO> getAllDocuments(Pageable pageable);
}