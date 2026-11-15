package com.eduquest.api.service.impl;

import com.eduquest.api.dto.request.ReportRequestDTO;
import com.eduquest.api.dto.response.PageResponseDTO;
import com.eduquest.api.dto.response.ReportResponseDTO;
import com.eduquest.api.entity.Document;
import com.eduquest.api.entity.Report;
import com.eduquest.api.entity.ReportStatus;
import com.eduquest.api.entity.ReportTargetType;
import com.eduquest.api.entity.User;
import com.eduquest.api.exception.BadRequestException;
import com.eduquest.api.exception.DuplicateResourceException;
import com.eduquest.api.exception.InvalidOperationException;
import com.eduquest.api.exception.ResourceNotFoundException;
import com.eduquest.api.repository.DocumentRepository;
import com.eduquest.api.repository.ReportRepository;
import com.eduquest.api.repository.UserRepository;
import com.eduquest.api.service.ReportService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

@Service
public class ReportServiceImpl implements ReportService {

    private final ReportRepository reportRepository;
    private final UserRepository userRepository;
    private final DocumentRepository documentRepository;

    public ReportServiceImpl(ReportRepository reportRepository,
                             UserRepository userRepository,
                             DocumentRepository documentRepository) {
        this.reportRepository = reportRepository;
        this.userRepository = userRepository;
        this.documentRepository = documentRepository;
    }

    @Override
    @Transactional
    public ReportResponseDTO createReport(ReportRequestDTO request, String username) {
        User reporter = findUser(username);

        String label;
        if (request.getTargetType() == ReportTargetType.USER) {
            User target = userRepository.findById(request.getTargetId())
                    .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
            if (target.getId().equals(reporter.getId())) {
                throw new InvalidOperationException("No puedes reportarte a ti mismo");
            }
            label = target.getUsername();
        } else {
            Document target = documentRepository.findById(request.getTargetId())
                    .orElseThrow(() -> new ResourceNotFoundException("Documento no encontrado"));
            label = target.getTitle();
        }

        if (reportRepository.existsByReporterIdAndTargetTypeAndTargetIdAndStatus(
                reporter.getId(), request.getTargetType(), request.getTargetId(), ReportStatus.OPEN)) {
            throw new DuplicateResourceException("Ya enviaste un reporte sobre esto; el equipo lo está revisando");
        }

        Report report = new Report();
        report.setTargetType(request.getTargetType());
        report.setTargetId(request.getTargetId());
        report.setTargetLabel(label);
        report.setReason(request.getReason().trim());
        report.setReporter(reporter);
        report.setCreatedAt(LocalDateTime.now());
        return toResponse(reportRepository.save(report));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponseDTO<ReportResponseDTO> getReports(String status, Pageable pageable) {
        Page<Report> page = status == null || status.isBlank()
                ? reportRepository.findAll(pageable)
                : reportRepository.findByStatus(parseStatus(status), pageable);
        List<ReportResponseDTO> content = page.getContent().stream().map(this::toResponse).toList();
        return PageResponseDTO.of(content, page.getNumber(), page.getSize(), page.getTotalElements());
    }

    @Override
    @Transactional(readOnly = true)
    public long countOpenReports() {
        return reportRepository.countByStatus(ReportStatus.OPEN);
    }

    @Override
    @Transactional
    public ReportResponseDTO updateStatus(Long reportId, String status, String moderatorUsername) {
        ReportStatus newStatus = parseStatus(status);
        if (newStatus == ReportStatus.OPEN) {
            throw new BadRequestException("El estado debe ser RESOLVED o DISMISSED");
        }
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new ResourceNotFoundException("Reporte no encontrado"));
        report.setStatus(newStatus);
        report.setResolvedAt(LocalDateTime.now());
        report.setResolvedBy(findUser(moderatorUsername));
        return toResponse(reportRepository.save(report));
    }

    private User findUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
    }

    private static ReportStatus parseStatus(String status) {
        try {
            return ReportStatus.valueOf(status.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException("Estado de reporte no válido: " + status);
        }
    }

    private ReportResponseDTO toResponse(Report report) {
        ReportResponseDTO dto = new ReportResponseDTO();
        dto.setId(report.getId());
        dto.setTargetType(report.getTargetType().name());
        dto.setTargetId(report.getTargetId());
        dto.setTargetLabel(report.getTargetLabel());
        dto.setReason(report.getReason());
        dto.setStatus(report.getStatus().name());
        dto.setReporterUsername(report.getReporter().getUsername());
        dto.setCreatedAt(report.getCreatedAt());
        dto.setResolvedAt(report.getResolvedAt());
        dto.setResolvedByUsername(report.getResolvedBy() != null ? report.getResolvedBy().getUsername() : null);
        return dto;
    }
}
