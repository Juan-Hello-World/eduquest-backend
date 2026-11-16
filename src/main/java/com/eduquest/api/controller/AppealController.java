package com.eduquest.api.controller;

import com.eduquest.api.dto.request.AppealDecisionRequestDTO;
import com.eduquest.api.dto.request.BanAppealRequestDTO;
import com.eduquest.api.dto.response.BanAppealResponseDTO;
import com.eduquest.api.dto.response.PageResponseDTO;
import com.eduquest.api.service.AppealService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@Tag(name = "Apelaciones", description = "Apelar una suspensión y revisarla desde la moderación.")
public class AppealController {

    private final AppealService appealService;

    public AppealController(AppealService appealService) {
        this.appealService = appealService;
    }

    @PostMapping("/api/v1/auth/appeals")
    @Operation(summary = "Apelar mi suspensión", description = "Público: pide usuario y contraseña porque la cuenta suspendida no puede iniciar sesión.")
    public ResponseEntity<BanAppealResponseDTO> submit(@Valid @RequestBody BanAppealRequestDTO request) {
        return new ResponseEntity<>(appealService.submit(request), HttpStatus.CREATED);
    }

    @GetMapping("/api/v1/admin/appeals")
    @PreAuthorize("hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_MANAGER')")
    @Operation(summary = "Bandeja de apelaciones", description = "status: PENDING, ACCEPTED o REJECTED; vacío para todas.")
    public ResponseEntity<PageResponseDTO<BanAppealResponseDTO>> list(@RequestParam(required = false) String status,
                                                                      @PageableDefault(size = 10) Pageable pageable) {
        return ResponseEntity.ok(appealService.list(status, pageable));
    }

    @GetMapping("/api/v1/admin/appeals/pending-count")
    @PreAuthorize("hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_MANAGER')")
    @Operation(summary = "Apelaciones pendientes")
    public ResponseEntity<Map<String, Long>> countPending() {
        return ResponseEntity.ok(Map.of("count", appealService.countPending()));
    }

    @PatchMapping("/api/v1/admin/appeals/{appealId}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_MANAGER')")
    @Operation(summary = "Resolver apelación", description = "Si se acepta, se levanta la suspensión y se avisa al usuario.")
    public ResponseEntity<BanAppealResponseDTO> decide(@PathVariable Long appealId,
                                                       @Valid @RequestBody AppealDecisionRequestDTO request,
                                                       Authentication authentication) {
        return ResponseEntity.ok(appealService.decide(appealId, request.getAccepted(), request.getResponse(),
                authentication.getName()));
    }
}
