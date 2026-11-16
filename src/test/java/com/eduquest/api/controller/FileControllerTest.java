package com.eduquest.api.controller;

import com.eduquest.api.dto.response.FileUploadResponseDTO;
import com.eduquest.api.dto.response.StoredFileDTO;
import com.eduquest.api.entity.StoredFile;
import com.eduquest.api.service.FileStorageService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.Authentication;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FileControllerTest {

    @Mock FileStorageService fileStorageService;

    @InjectMocks FileController fileController;

    @BeforeEach
    void setUp() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setScheme("http");
        request.setServerName("localhost");
        request.setServerPort(8080);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void upload_returnsPublicUrl() {
        Authentication authentication = mock(Authentication.class);
        when(authentication.getName()).thenReturn("alice");
        StoredFile stored = new StoredFile();
        stored.setId("abc");
        stored.setFileName("apuntes.pdf");
        stored.setContentType("application/pdf");
        stored.setSize(3);
        when(fileStorageService.store(any(), eq("alice"))).thenReturn(stored);

        ResponseEntity<FileUploadResponseDTO> response = fileController.upload(
                new MockMultipartFile("file", "apuntes.pdf", "application/pdf", new byte[]{1}), authentication);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody().url()).isEqualTo("http://localhost:8080/api/v1/files/abc/apuntes.pdf");
    }

    @Test
    void download_inlineOrAttachment() throws java.io.IOException {
        when(fileStorageService.load("abc")).thenReturn(new StoredFileDTO(new byte[]{1}, "application/pdf", "apuntes.pdf"));

        ResponseEntity<org.springframework.core.io.Resource> inline = fileController.download("abc", false);
        ResponseEntity<org.springframework.core.io.Resource> attachment = fileController.download("abc", true);

        assertThat(inline.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION)).startsWith("inline");
        assertThat(attachment.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION)).startsWith("attachment");
        assertThat(inline.getBody().getContentAsByteArray()).containsExactly(1);
        assertThat(inline.getHeaders().getFirst(HttpHeaders.ACCEPT_RANGES)).isEqualTo("bytes");
    }
}
