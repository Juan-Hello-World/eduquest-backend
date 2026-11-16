package com.eduquest.api.controller;

import com.eduquest.api.dto.response.GlobalSearchDTO;
import com.eduquest.api.service.SearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/search")
@PreAuthorize("hasAuthority('ROLE_USER') or hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_MANAGER')")
@Tag(name = "Búsqueda global", description = "Materiales, grupos, preguntas del foro y personas en una sola búsqueda.")
public class SearchController {

    private final SearchService searchService;

    public SearchController(SearchService searchService) {
        this.searchService = searchService;
    }

    @GetMapping
    @Operation(summary = "Buscar en EduQuest", description = "Hasta 5 resultados por categoría; mínimo 2 caracteres.")
    public ResponseEntity<GlobalSearchDTO> search(@RequestParam String q, Authentication authentication) {
        return ResponseEntity.ok(searchService.search(q, authentication.getName()));
    }
}
