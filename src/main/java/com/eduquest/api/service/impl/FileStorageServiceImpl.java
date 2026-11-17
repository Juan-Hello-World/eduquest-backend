package com.eduquest.api.service.impl;

import com.eduquest.api.dto.response.StoredFileDTO;
import com.eduquest.api.entity.StoredFile;
import com.eduquest.api.entity.User;
import com.eduquest.api.exception.BadRequestException;
import com.eduquest.api.exception.ResourceNotFoundException;
import com.eduquest.api.repository.StoredFileRepository;
import com.eduquest.api.repository.UserRepository;
import com.eduquest.api.service.FileStorageService;
import com.eduquest.api.storage.ObjectStorage;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.text.Normalizer;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class FileStorageServiceImpl implements FileStorageService {

    public static final long MAX_FILE_BYTES = 10L * 1024 * 1024;
    public static final long MAX_IMAGE_BYTES = 25L * 1024 * 1024;
    public static final long MAX_VIDEO_BYTES = 50L * 1024 * 1024;

    private static final Map<String, String> ALLOWED_TYPES = Map.ofEntries(
            Map.entry("application/pdf", "pdf"),
            Map.entry("application/msword", "doc"),
            Map.entry("application/vnd.openxmlformats-officedocument.wordprocessingml.document", "docx"),
            Map.entry("application/vnd.ms-powerpoint", "ppt"),
            Map.entry("application/vnd.openxmlformats-officedocument.presentationml.presentation", "pptx"),
            Map.entry("application/vnd.ms-excel", "xls"),
            Map.entry("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "xlsx"),
            Map.entry("text/plain", "txt"),
            Map.entry("image/png", "png"),
            Map.entry("image/jpeg", "jpg"),
            Map.entry("image/webp", "webp"),
            Map.entry("image/gif", "gif"),
            Map.entry("video/mp4", "mp4"),
            Map.entry("video/webm", "webm"),
            Map.entry("video/quicktime", "mov"));

    private static final Pattern STORED_URL = Pattern.compile("/api/v1/files/([0-9a-f-]{36})(?:/|$)");

    private final StoredFileRepository fileRepository;
    private final UserRepository userRepository;
    private final ObjectProvider<ObjectStorage> objectStorage;

    public FileStorageServiceImpl(StoredFileRepository fileRepository,
                                  UserRepository userRepository,
                                  ObjectProvider<ObjectStorage> objectStorage) {
        this.fileRepository = fileRepository;
        this.userRepository = userRepository;
        this.objectStorage = objectStorage;
    }

    @Override
    @Transactional
    public StoredFile store(MultipartFile file, String username) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Selecciona un archivo");
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_TYPES.containsKey(contentType)) {
            throw new BadRequestException("Formato no permitido. Sube un PDF, Word, PowerPoint, Excel, texto, imagen o video");
        }
        if (contentType.startsWith("video/") && file.getSize() > MAX_VIDEO_BYTES) {
            throw new BadRequestException("El video no debe superar los 50 MB");
        }
        if (contentType.startsWith("image/") && file.getSize() > MAX_IMAGE_BYTES) {
            throw new BadRequestException("La imagen no debe superar los 25 MB");
        }
        if (!contentType.startsWith("video/") && !contentType.startsWith("image/") && file.getSize() > MAX_FILE_BYTES) {
            throw new BadRequestException("El archivo no debe superar los 10 MB");
        }
        User uploader = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        StoredFile stored = new StoredFile();
        stored.setId(UUID.randomUUID().toString());
        stored.setContentType(contentType);
        stored.setFileName(safeFileName(file.getOriginalFilename(), ALLOWED_TYPES.get(contentType)));
        byte[] data = readBytes(file);
        ObjectStorage storage = objectStorage.getIfAvailable();
        if (storage != null) {
            String key = "files/" + stored.getId() + "/" + stored.getFileName();
            storage.put(key, data, contentType);
            stored.setStorageKey(key);
        } else {
            stored.setData(data);
        }
        stored.setSize(file.getSize());
        stored.setUploader(uploader);
        stored.setCreatedAt(LocalDateTime.now());
        return fileRepository.save(stored);
    }

    @Override
    @Transactional(readOnly = true)
    public StoredFileDTO load(String fileId) {
        StoredFile file = fileRepository.findById(fileId)
                .orElseThrow(() -> new ResourceNotFoundException("Archivo no encontrado"));
        return toDto(file);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<String> temporaryUrl(String fileId, boolean download) {
        StoredFile file = fileRepository.findById(fileId)
                .orElseThrow(() -> new ResourceNotFoundException("Archivo no encontrado"));
        ObjectStorage storage = objectStorage.getIfAvailable();
        if (storage == null || file.getStorageKey() == null) {
            return Optional.empty();
        }
        return Optional.of(storage.temporaryUrl(file.getStorageKey(), file.getFileName(), download));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<StoredFileDTO> loadByUrl(String fileUrl) {
        if (fileUrl == null) {
            return Optional.empty();
        }
        Matcher matcher = STORED_URL.matcher(fileUrl);
        if (!matcher.find()) {
            return Optional.empty();
        }
        return fileRepository.findById(matcher.group(1)).map(this::toDto);
    }

    @Override
    @Transactional
    public void deleteByUrl(String fileUrl) {
        if (fileUrl == null) {
            return;
        }
        Matcher matcher = STORED_URL.matcher(fileUrl);
        if (!matcher.find()) {
            return;
        }
        fileRepository.findById(matcher.group(1)).ifPresent(file -> {
            ObjectStorage storage = objectStorage.getIfAvailable();
            if (storage != null && file.getStorageKey() != null) {
                storage.delete(file.getStorageKey());
            }
            fileRepository.delete(file);
        });
    }

    private StoredFileDTO toDto(StoredFile file) {
        byte[] data = file.getData();
        ObjectStorage storage = objectStorage.getIfAvailable();
        if (data == null && storage != null && file.getStorageKey() != null) {
            data = storage.get(file.getStorageKey());
        }
        return new StoredFileDTO(data, file.getContentType(), file.getFileName());
    }

    static String safeFileName(String original, String extension) {
        String base = original == null ? "" : original.replaceAll("\\.[^.]*$", "");
        String clean = Normalizer.normalize(base, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replaceAll("[^A-Za-z0-9._-]+", "-")
                .replaceAll("-{2,}", "-")
                .replaceAll("^[-.]+|[-.]+$", "");
        if (clean.isEmpty()) {
            clean = "archivo";
        }
        if (clean.length() > 80) {
            clean = clean.substring(0, 80);
        }
        return clean + "." + extension;
    }

    private static byte[] readBytes(MultipartFile file) {
        try {
            return file.getBytes();
        } catch (IOException ex) {
            throw new BadRequestException("No se pudo leer el archivo");
        }
    }
}
