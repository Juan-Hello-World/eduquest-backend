package com.eduquest.api.service;

import com.eduquest.api.dto.response.StoredFileDTO;
import com.eduquest.api.entity.StoredFile;
import org.springframework.web.multipart.MultipartFile;

import java.util.Optional;

public interface FileStorageService {
    StoredFile store(MultipartFile file, String username);
    StoredFileDTO load(String fileId);
    void deleteByUrl(String fileUrl);
    Optional<StoredFileDTO> loadByUrl(String fileUrl);
    Optional<String> temporaryUrl(String fileId, boolean download);
}
