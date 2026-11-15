package com.eduquest.api.controller;

import com.eduquest.api.dto.request.DocumentRequestDTO;
import com.eduquest.api.dto.response.DocumentResponseDTO;
import com.eduquest.api.dto.response.PageResponseDTO;
import com.eduquest.api.service.DocumentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/documents")
@Tag(name = "Documentos", description = "Subida y consulta de documentos académicos (globales o de un grupo).")
public class DocumentController {

    private final DocumentService documentService;

    public DocumentController(DocumentService documentService) {
        this.documentService = documentService;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('ROLE_USER') or hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_MANAGER')")
    @Operation(summary = "Subir documento (global)", description = "Crea un documento general. Para subirlo a un grupo usar POST /api/v1/groups/{groupId}/documents.")
    public ResponseEntity<DocumentResponseDTO> createDocument(
            @Valid @RequestBody DocumentRequestDTO request,
            Authentication authentication) {

        String username = authentication.getName();
        DocumentResponseDTO response = documentService.createDocument(request, username);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping
    @Operation(summary = "Listar documentos", description = "Devuelve documentos paginados. Permite filtrar por texto (search), etiqueta (tag), curso (course) y universidad (university), y ordenar con sort: recent (por defecto), oldest o title.")
    public ResponseEntity<PageResponseDTO<DocumentResponseDTO>> getAllDocuments(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String tag,
            @RequestParam(required = false) String course,
            @RequestParam(required = false) String university,
            @RequestParam(required = false) String sort) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100), toSort(sort));
        return new ResponseEntity<>(documentService.getAllDocuments(search, tag, course, university, pageable), HttpStatus.OK);
    }

    @GetMapping("/saved")
    @Operation(summary = "Mis guardados", description = "Documentos que el usuario guardó como favoritos.")
    public ResponseEntity<PageResponseDTO<DocumentResponseDTO>> getSavedDocuments(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Authentication authentication) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100));
        return ResponseEntity.ok(documentService.getSavedDocuments(authentication.getName(), pageable));
    }

    @GetMapping("/saved/ids")
    @Operation(summary = "Ids de mis guardados", description = "Sirve para marcar en las listas qué documentos ya están guardados.")
    public ResponseEntity<List<Long>> getSavedDocumentIds(Authentication authentication) {
        return ResponseEntity.ok(documentService.getSavedDocumentIds(authentication.getName()));
    }

    @PostMapping("/{documentId}/saved")
    @Operation(summary = "Guardar documento")
    public ResponseEntity<Void> saveDocument(@PathVariable Long documentId, Authentication authentication) {
        documentService.saveDocument(documentId, authentication.getName());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{documentId}/saved")
    @Operation(summary = "Quitar de guardados")
    public ResponseEntity<Void> unsaveDocument(@PathVariable Long documentId, Authentication authentication) {
        documentService.unsaveDocument(documentId, authentication.getName());
        return ResponseEntity.noContent().build();
    }

    private static Sort toSort(String sort) {
        if ("oldest".equalsIgnoreCase(sort)) {
            return Sort.by(Sort.Direction.ASC, "uploadedAt");
        }
        if ("title".equalsIgnoreCase(sort)) {
            return Sort.by(Sort.Direction.ASC, "title");
        }
        return Sort.by(Sort.Direction.DESC, "uploadedAt");
    }
}
