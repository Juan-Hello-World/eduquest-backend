package com.eduquest.api.service.impl;

import com.eduquest.api.dto.request.ReportRequestDTO;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ReportServiceImplTest {

    @Mock ReportRepository reportRepository;
    @Mock UserRepository userRepository;
    @Mock DocumentRepository documentRepository;

    @InjectMocks ReportServiceImpl reportService;

    private User alice;
    private User bob;

    @BeforeEach
    void setUp() {
        alice = new User();
        alice.setId(1L);
        alice.setUsername("alice");
        bob = new User();
        bob.setId(2L);
        bob.setUsername("bob");
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(alice));
        when(reportRepository.save(any(Report.class))).thenAnswer(inv -> {
            Report saved = inv.getArgument(0);
            if (saved.getId() == null) {
                saved.setId(20L);
            }
            return saved;
        });
    }

    private ReportRequestDTO request(ReportTargetType type, long id) {
        ReportRequestDTO request = new ReportRequestDTO();
        request.setTargetType(type);
        request.setTargetId(id);
        request.setReason("  Contenido ofensivo en la descripción  ");
        return request;
    }

    @Test
    void createReport_forUser_storesUsernameAsLabel() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(bob));

        ReportResponseDTO dto = reportService.createReport(request(ReportTargetType.USER, 2L), "alice");

        assertThat(dto.getTargetLabel()).isEqualTo("bob");
        assertThat(dto.getStatus()).isEqualTo("OPEN");
        assertThat(dto.getReason()).isEqualTo("Contenido ofensivo en la descripción");
        assertThat(dto.getReporterUsername()).isEqualTo("alice");
    }

    @Test
    void createReport_forDocument_storesTitleAsLabel() {
        Document document = new Document();
        document.setId(3L);
        document.setTitle("Apuntes");
        when(documentRepository.findById(3L)).thenReturn(Optional.of(document));

        assertThat(reportService.createReport(request(ReportTargetType.DOCUMENT, 3L), "alice").getTargetLabel())
                .isEqualTo("Apuntes");
    }

    @Test
    void createReport_againstYourself_throwsInvalidOperation() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(alice));

        assertThatThrownBy(() -> reportService.createReport(request(ReportTargetType.USER, 1L), "alice"))
                .isInstanceOf(InvalidOperationException.class);
    }

    @Test
    void createReport_missingTargets_throwNotFound() {
        when(userRepository.findById(9L)).thenReturn(Optional.empty());
        when(documentRepository.findById(9L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reportService.createReport(request(ReportTargetType.USER, 9L), "alice"))
                .isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> reportService.createReport(request(ReportTargetType.DOCUMENT, 9L), "alice"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void createReport_whenOpenReportExists_throwsDuplicate() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(bob));
        when(reportRepository.existsByReporterIdAndTargetTypeAndTargetIdAndStatus(1L, ReportTargetType.USER, 2L, ReportStatus.OPEN))
                .thenReturn(true);

        assertThatThrownBy(() -> reportService.createReport(request(ReportTargetType.USER, 2L), "alice"))
                .isInstanceOf(DuplicateResourceException.class);
    }

    private Report storedReport() {
        Report report = new Report();
        report.setId(20L);
        report.setTargetType(ReportTargetType.USER);
        report.setTargetId(2L);
        report.setTargetLabel("bob");
        report.setReason("Spam en el chat del grupo");
        report.setReporter(alice);
        report.setCreatedAt(LocalDateTime.now());
        return report;
    }

    @Test
    void getReports_filtersByStatusOrReturnsAll() {
        Pageable pageable = PageRequest.of(0, 10);
        when(reportRepository.findByStatus(ReportStatus.OPEN, pageable)).thenReturn(new PageImpl<>(List.of(storedReport()), pageable, 1));
        when(reportRepository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(storedReport(), storedReport()), pageable, 2));

        assertThat(reportService.getReports("open", pageable).content()).hasSize(1);
        assertThat(reportService.getReports(null, pageable).content()).hasSize(2);
        assertThatThrownBy(() -> reportService.getReports("raro", pageable)).isInstanceOf(BadRequestException.class);
    }

    @Test
    void countOpenReports_delegatesToRepository() {
        when(reportRepository.countByStatus(ReportStatus.OPEN)).thenReturn(4L);

        assertThat(reportService.countOpenReports()).isEqualTo(4L);
    }

    @Test
    void updateStatus_closesReportWithModerator() {
        when(reportRepository.findById(20L)).thenReturn(Optional.of(storedReport()));

        ReportResponseDTO dto = reportService.updateStatus(20L, "RESOLVED", "alice");

        assertThat(dto.getStatus()).isEqualTo("RESOLVED");
        assertThat(dto.getResolvedByUsername()).isEqualTo("alice");
        assertThat(dto.getResolvedAt()).isNotNull();
    }

    @Test
    void updateStatus_toOpenOrMissing_fails() {
        when(reportRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reportService.updateStatus(20L, "OPEN", "alice")).isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> reportService.updateStatus(99L, "DISMISSED", "alice"))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
