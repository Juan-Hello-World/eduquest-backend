package com.eduquest.api.service;

import com.eduquest.api.dto.request.DocumentRequestDTO;
import com.eduquest.api.dto.response.DocumentResponseDTO;
import java.util.List;

public interface DocumentService {
    DocumentResponseDTO createDocument(DocumentRequestDTO request, String username);
    List<DocumentResponseDTO> getAllDocuments();
}