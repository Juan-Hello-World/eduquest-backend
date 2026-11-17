package com.eduquest.api.service.impl;

import com.eduquest.api.dto.response.StoredFileDTO;
import com.eduquest.api.entity.StoredFile;
import com.eduquest.api.entity.User;
import com.eduquest.api.exception.BadRequestException;
import com.eduquest.api.exception.ResourceNotFoundException;
import com.eduquest.api.repository.StoredFileRepository;
import com.eduquest.api.repository.UserRepository;
import com.eduquest.api.storage.ObjectStorage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class FileStorageServiceImplTest {

    @Mock StoredFileRepository fileRepository;
    @Mock UserRepository userRepository;
    @Mock ObjectProvider<ObjectStorage> objectStorage;
    @Mock ObjectStorage storage;

    @InjectMocks FileStorageServiceImpl fileStorageService;

    private User alice;

    @BeforeEach
    void setUp() {
        alice = new User();
        alice.setId(1L);
        alice.setUsername("alice");
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(alice));
        when(fileRepository.save(any(StoredFile.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void store_validPdf_savesWithCleanName() {
        MockMultipartFile file = new MockMultipartFile("file", "Apuntes de Cálculo (final).pdf",
                "application/pdf", new byte[]{1, 2, 3});

        StoredFile stored = fileStorageService.store(file, "alice");

        assertThat(stored.getId()).hasSize(36);
        assertThat(stored.getFileName()).isEqualTo("Apuntes-de-Calculo-final.pdf");
        assertThat(stored.getSize()).isEqualTo(3);
        assertThat(stored.getUploader()).isEqualTo(alice);
    }

    @Test
    void store_rejectsEmptyLargeOrUnknownFiles() {
        assertThatThrownBy(() -> fileStorageService.store(null, "alice")).isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> fileStorageService.store(
                new MockMultipartFile("file", "a.pdf", "application/pdf", new byte[0]), "alice"))
                .isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> fileStorageService.store(
                new MockMultipartFile("file", "a.html", "text/html", new byte[]{1}), "alice"))
                .isInstanceOf(BadRequestException.class);
        MultipartFile huge = mock(MultipartFile.class);
        when(huge.isEmpty()).thenReturn(false);
        when(huge.getContentType()).thenReturn("application/pdf");
        when(huge.getSize()).thenReturn(FileStorageServiceImpl.MAX_FILE_BYTES + 1);
        assertThatThrownBy(() -> fileStorageService.store(huge, "alice"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("10 MB");
    }

    @Test
    void store_acceptsVideosUpToTheirOwnLimit() throws IOException {
        when(fileRepository.save(org.mockito.ArgumentMatchers.any(StoredFile.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        MultipartFile clip = mock(MultipartFile.class);
        when(clip.isEmpty()).thenReturn(false);
        when(clip.getContentType()).thenReturn("video/mp4");
        when(clip.getOriginalFilename()).thenReturn("Exposición final.mp4");
        when(clip.getSize()).thenReturn(FileStorageServiceImpl.MAX_FILE_BYTES + 1);
        when(clip.getBytes()).thenReturn(new byte[]{1, 2});

        StoredFile stored = fileStorageService.store(clip, "alice");

        assertThat(stored.getFileName()).isEqualTo("Exposicion-final.mp4");
        assertThat(stored.getContentType()).isEqualTo("video/mp4");

        MultipartFile photo = mock(MultipartFile.class);
        when(photo.isEmpty()).thenReturn(false);
        when(photo.getContentType()).thenReturn("image/png");
        when(photo.getOriginalFilename()).thenReturn("pizarra.png");
        when(photo.getSize()).thenReturn(FileStorageServiceImpl.MAX_FILE_BYTES + 1);
        when(photo.getBytes()).thenReturn(new byte[]{1});
        assertThat(fileStorageService.store(photo, "alice").getFileName()).isEqualTo("pizarra.png");
        when(photo.getSize()).thenReturn(FileStorageServiceImpl.MAX_IMAGE_BYTES + 1);
        assertThatThrownBy(() -> fileStorageService.store(photo, "alice"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("25 MB");

        when(clip.getSize()).thenReturn(FileStorageServiceImpl.MAX_VIDEO_BYTES + 1);
        assertThatThrownBy(() -> fileStorageService.store(clip, "alice"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("50 MB");
    }

    @Test
    void store_unreadableFileOrUnknownUser_fails() throws IOException {
        MultipartFile broken = mock(MultipartFile.class);
        when(broken.isEmpty()).thenReturn(false);
        when(broken.getSize()).thenReturn(10L);
        when(broken.getContentType()).thenReturn("image/png");
        when(broken.getBytes()).thenThrow(new IOException("boom"));
        assertThatThrownBy(() -> fileStorageService.store(broken, "alice")).isInstanceOf(BadRequestException.class);

        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> fileStorageService.store(
                new MockMultipartFile("file", "a.txt", "text/plain", new byte[]{1}), "ghost"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void load_returnsDataOrNotFound() {
        StoredFile stored = new StoredFile();
        stored.setData(new byte[]{9});
        stored.setContentType("image/png");
        stored.setFileName("foto.png");
        when(fileRepository.findById("abc")).thenReturn(Optional.of(stored));
        when(fileRepository.findById("nope")).thenReturn(Optional.empty());

        StoredFileDTO dto = fileStorageService.load("abc");

        assertThat(dto.fileName()).isEqualTo("foto.png");
        assertThatThrownBy(() -> fileStorageService.load("nope")).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void deleteByUrl_onlyDeletesOwnStoredFiles() {
        String id = "0f8c6a43-6a1e-4c55-9a6f-9a7d5a1b2c3d";
        StoredFile stored = new StoredFile();
        stored.setId(id);
        when(fileRepository.findById(id)).thenReturn(Optional.of(stored));

        fileStorageService.deleteByUrl("http://localhost:8080/api/v1/files/" + id + "/apuntes.pdf");
        fileStorageService.deleteByUrl("https://drive.google.com/file/d/123/view");
        fileStorageService.deleteByUrl(null);

        verify(fileRepository).delete(stored);
        verify(fileRepository, never()).findById("123");
    }

    @Test
    void store_withObjectStorage_uploadsBytesAndKeepsOnlyTheKey() {
        when(objectStorage.getIfAvailable()).thenReturn(storage);
        MockMultipartFile file = new MockMultipartFile("file", "resumen.pdf", "application/pdf", new byte[]{4, 5});

        StoredFile stored = fileStorageService.store(file, "alice");

        assertThat(stored.getData()).isNull();
        assertThat(stored.getStorageKey()).isEqualTo("files/" + stored.getId() + "/resumen.pdf");
        verify(storage).put(eq(stored.getStorageKey()), eq(new byte[]{4, 5}), eq("application/pdf"));
    }

    @Test
    void temporaryUrl_onlyForFilesInObjectStorage() {
        StoredFile inS3 = new StoredFile();
        inS3.setStorageKey("files/abc/resumen.pdf");
        inS3.setFileName("resumen.pdf");
        StoredFile inDatabase = new StoredFile();
        inDatabase.setData(new byte[]{1});
        when(fileRepository.findById("abc")).thenReturn(Optional.of(inS3));
        when(fileRepository.findById("db")).thenReturn(Optional.of(inDatabase));
        when(fileRepository.findById("nope")).thenReturn(Optional.empty());

        assertThat(fileStorageService.temporaryUrl("abc", false)).isEmpty();

        when(objectStorage.getIfAvailable()).thenReturn(storage);
        when(storage.temporaryUrl("files/abc/resumen.pdf", "resumen.pdf", true)).thenReturn("https://s3/firmada");

        assertThat(fileStorageService.temporaryUrl("abc", true)).contains("https://s3/firmada");
        assertThat(fileStorageService.temporaryUrl("db", true)).isEmpty();
        assertThatThrownBy(() -> fileStorageService.temporaryUrl("nope", true)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void loadAndDelete_withObjectStorage_readAndRemoveTheObject() {
        String id = "0f8c6a43-6a1e-4c55-9a6f-9a7d5a1b2c3d";
        StoredFile stored = new StoredFile();
        stored.setId(id);
        stored.setStorageKey("files/" + id + "/foto.png");
        stored.setContentType("image/png");
        stored.setFileName("foto.png");
        when(fileRepository.findById(id)).thenReturn(Optional.of(stored));
        when(objectStorage.getIfAvailable()).thenReturn(storage);
        when(storage.get("files/" + id + "/foto.png")).thenReturn(new byte[]{8});

        assertThat(fileStorageService.loadByUrl("https://api/api/v1/files/" + id + "/foto.png"))
                .hasValueSatisfying(dto -> assertThat(dto.data()).containsExactly(8));

        fileStorageService.deleteByUrl("https://api/api/v1/files/" + id + "/foto.png");

        verify(storage).delete("files/" + id + "/foto.png");
        verify(fileRepository).delete(stored);
    }

    @Test
    void safeFileName_fallsBackAndTruncates() {
        assertThat(FileStorageServiceImpl.safeFileName(null, "pdf")).isEqualTo("archivo.pdf");
        assertThat(FileStorageServiceImpl.safeFileName("***", "txt")).isEqualTo("archivo.txt");
        assertThat(FileStorageServiceImpl.safeFileName("a".repeat(120) + ".docx", "docx")).hasSize(85);
    }
}
