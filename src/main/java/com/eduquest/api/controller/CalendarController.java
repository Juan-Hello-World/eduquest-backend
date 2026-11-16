package com.eduquest.api.controller;

import com.eduquest.api.service.CalendarExportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/v1/sessions/mine/calendar.ics")
@PreAuthorize("hasAuthority('ROLE_USER') or hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_MANAGER')")
@Tag(name = "Calendario", description = "Exporta las asesorías de mis grupos para Google Calendar u Outlook.")
public class CalendarController {

    private final CalendarExportService calendarExportService;

    public CalendarController(CalendarExportService calendarExportService) {
        this.calendarExportService = calendarExportService;
    }

    @GetMapping
    @Operation(summary = "Exportar asesorías (.ics)", description = "Del último mes a los próximos 6 meses, con recordatorio 30 minutos antes.")
    public ResponseEntity<byte[]> export(Authentication authentication) {
        byte[] body = calendarExportService.exportMySessions(authentication.getName()).getBytes(StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .contentType(new MediaType("text", "calendar", StandardCharsets.UTF_8))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename("eduquest-asesorias.ics").build().toString())
                .body(body);
    }
}
