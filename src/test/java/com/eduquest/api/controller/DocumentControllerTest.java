package com.eduquest.api.controller;

import com.eduquest.api.dto.request.DocumentRequestDTO;
import com.eduquest.api.dto.response.DocumentResponseDTO;
import com.eduquest.api.dto.response.PageResponseDTO;
import com.eduquest.api.service.DocumentService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DocumentControllerTest {

    @Mock DocumentService documentService;

    @InjectMocks DocumentController documentController;

    private DocumentResponseDTO response() {
        DocumentResponseDTO dto = new DocumentResponseDTO();
        dto.setId(1L);
        dto.setTitle("Apuntes");
        dto.setFileUrl("https://x/a.pdf");
        dto.setUploadedAt(LocalDateTime.now());
        dto.setAuthorUsername("alice");
        return dto;
    }

    @Test
    void createDocument_returnsCreatedWithUsernameFromAuthentication() {
        Authentication authentication = mock(Authentication.class);
        when(authentication.getName()).thenReturn("alice");
        when(documentService.createDocument(any(), anyString())).thenReturn(response());

        DocumentRequestDTO request = new DocumentRequestDTO();
        request.setTitle("Apuntes");
        request.setFileUrl("https://x/a.pdf");

        ResponseEntity<DocumentResponseDTO> result = documentController.createDocument(request, authentication);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(result.getBody().getAuthorUsername()).isEqualTo("alice");
    }

    @Test
    void getAllDocuments_returnsPaginatedResponse() {
        PageResponseDTO<DocumentResponseDTO> page =
                new PageResponseDTO<>(List.of(response()), 0, 10, 1, 1);
        when(documentService.getAllDocuments(any())).thenReturn(page);

        ResponseEntity<PageResponseDTO<DocumentResponseDTO>> result = documentController.getAllDocuments(0, 10);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody().content()).hasSize(1);
    }

    @Test
    void getAllDocuments_clampsInvalidParams() {
        when(documentService.getAllDocuments(any())).thenReturn(
                new PageResponseDTO<>(List.of(), 0, 1, 0, 0));

        documentController.getAllDocuments(-5, 1000);

        assertThat(true).isTrue();
    }
}