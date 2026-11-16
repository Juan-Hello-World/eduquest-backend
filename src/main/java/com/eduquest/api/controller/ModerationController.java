package com.eduquest.api.controller;

import com.eduquest.api.dto.request.AnnouncementRequestDTO;
import com.eduquest.api.dto.request.ReportStatusRequestDTO;
import com.eduquest.api.dto.response.AdminStatsDTO;
import com.eduquest.api.dto.response.DocumentResponseDTO;
import com.eduquest.api.dto.response.PageResponseDTO;
import com.eduquest.api.dto.response.ReportResponseDTO;
import com.eduquest.api.service.AdminService;
import com.eduquest.api.service.DocumentService;
import com.eduquest.api.service.NotificationService;
import com.eduquest.api.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin")
@PreAuthorize("hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_MANAGER')")
@Tag(name = "Moderación", description = "Reportes, materiales destacados, eliminación de material y estadísticas (ADMIN o MANAGER). Los avisos generales son solo de ROLE_ADMIN.")
public class ModerationController {

    private final ReportService reportService;
    private final DocumentService documentService;
    private final NotificationService notificationService;
    private final AdminService adminService;

    public ModerationController(ReportService reportService, DocumentService documentService,
                                NotificationService notificationService, AdminService adminService) {
        this.reportService = reportService;
        this.documentService = documentService;
        this.notificationService = notificationService;
        this.adminService = adminService;
    }

    @PostMapping("/notifications")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @Operation(summary = "Enviar aviso", description = "Solo ADMIN. Sin username llega a todos los usuarios activos; con username, solo a esa persona.")
    public ResponseEntity<Map<String, Integer>> announce(@Valid @RequestBody AnnouncementRequestDTO request) {
        return ResponseEntity.ok(Map.of("recipients", notificationService.announce(request)));
    }

    @GetMapping("/reports")
    @Operation(summary = "Listar reportes", description = "status opcional: OPEN, RESOLVED o DISMISSED. Más recientes primero.")
    public ResponseEntity<PageResponseDTO<ReportResponseDTO>> getReports(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100),
                Sort.by(Sort.Direction.DESC, "createdAt"));
        return ResponseEntity.ok(reportService.getReports(status, pageable));
    }

    @GetMapping("/reports/open-count")
    @Operation(summary = "Cantidad de reportes pendientes")
    public ResponseEntity<Map<String, Long>> countOpenReports() {
        return ResponseEntity.ok(Map.of("count", reportService.countOpenReports()));
    }

    @PatchMapping("/reports/{reportId}")
    @Operation(summary = "Cerrar reporte", description = "status: RESOLVED (se tomó acción) o DISMISSED (no incumple las normas).")
    public ResponseEntity<ReportResponseDTO> updateReport(@PathVariable Long reportId,
                                                          @Valid @RequestBody ReportStatusRequestDTO request,
                                                          Authentication authentication) {
        return ResponseEntity.ok(reportService.updateStatus(reportId, request.getStatus(), authentication.getName()));
    }

    @GetMapping("/stats")
    @Operation(summary = "Resumen de la plataforma", description = "Usuarios, materiales, grupos, sesiones y reportes abiertos.")
    public ResponseEntity<AdminStatsDTO> getStats() {
        return ResponseEntity.ok(adminService.getStats());
    }

    @PatchMapping("/documents/{documentId}/featured")
    @Operation(summary = "Destacar material", description = "ADMIN o MANAGER. Con featured=false lo quita de destacados.")
    public ResponseEntity<DocumentResponseDTO> setFeatured(@PathVariable Long documentId,
                                                           @RequestParam(defaultValue = "true") boolean featured) {
        return ResponseEntity.ok(documentService.setFeatured(documentId, featured));
    }

    @DeleteMapping("/documents/{documentId}")
    @Operation(summary = "Eliminar material", description = "ADMIN o MANAGER. Borra el documento junto con sus reseñas.")
    public ResponseEntity<Void> deleteDocument(@PathVariable Long documentId) {
        documentService.deleteDocument(documentId);
        return ResponseEntity.noContent().build();
    }
}
