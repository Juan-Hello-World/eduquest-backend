package com.eduquest.api.controller;

import com.eduquest.api.dto.response.FileUploadResponseDTO;
import com.eduquest.api.dto.response.StoredFileDTO;
import com.eduquest.api.entity.StoredFile;
import com.eduquest.api.service.FileStorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/v1/files")
@Tag(name = "Archivos", description = "Subida de archivos para los materiales. Devuelve la URL pública que se usa como fileUrl.")
public class FileController {

    private final FileStorageService fileStorageService;

    public FileController(FileStorageService fileStorageService) {
        this.fileStorageService = fileStorageService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('ROLE_USER') or hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_MANAGER')")
    @Operation(summary = "Subir archivo",
            description = "PDF, Word, PowerPoint, Excel o texto de hasta 10 MB, imagen de hasta 25 MB, o video MP4/WebM/MOV de hasta 50 MB, en el campo 'file'.")
    public ResponseEntity<FileUploadResponseDTO> upload(@RequestParam("file") MultipartFile file,
                                                        Authentication authentication) {
        StoredFile stored = fileStorageService.store(file, authentication.getName());
        String url = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/v1/files/{id}/{name}")
                .buildAndExpand(stored.getId(), stored.getFileName())
                .toUriString();
        return new ResponseEntity<>(
                new FileUploadResponseDTO(url, stored.getFileName(), stored.getContentType(), stored.getSize()),
                HttpStatus.CREATED);
    }

    @GetMapping({"/{fileId}", "/{fileId}/{fileName}"})
    @Operation(summary = "Descargar archivo",
            description = "Público. Con download=true el navegador lo descarga en lugar de mostrarlo. Admite Range para adelantar videos.")
    public ResponseEntity<Resource> download(@PathVariable String fileId,
                                           @RequestParam(defaultValue = "false") boolean download) {
        StoredFileDTO file = fileStorageService.load(fileId);
        ContentDisposition disposition = (download ? ContentDisposition.attachment() : ContentDisposition.inline())
                .filename(file.fileName(), StandardCharsets.UTF_8)
                .build();
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(file.contentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .cacheControl(CacheControl.maxAge(30, TimeUnit.DAYS).cachePublic())
                .header(HttpHeaders.ACCEPT_RANGES, "bytes")
                .body(new ByteArrayResource(file.data()));
    }
}
