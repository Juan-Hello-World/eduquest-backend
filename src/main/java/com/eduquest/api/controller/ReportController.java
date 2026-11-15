package com.eduquest.api.controller;

import com.eduquest.api.dto.request.ReportRequestDTO;
import com.eduquest.api.dto.response.ReportResponseDTO;
import com.eduquest.api.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/reports")
@Tag(name = "Reportes", description = "Reportar documentos o usuarios que incumplen las normas.")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @PostMapping
    @Operation(summary = "Reportar contenido", description = "targetType: DOCUMENT o USER. 409 si ya tienes un reporte abierto sobre lo mismo.")
    public ResponseEntity<ReportResponseDTO> createReport(@Valid @RequestBody ReportRequestDTO request,
                                                          Authentication authentication) {
        return new ResponseEntity<>(reportService.createReport(request, authentication.getName()), HttpStatus.CREATED);
    }
}
