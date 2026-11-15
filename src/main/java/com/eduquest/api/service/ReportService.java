package com.eduquest.api.service;

import com.eduquest.api.dto.request.ReportRequestDTO;
import com.eduquest.api.dto.response.PageResponseDTO;
import com.eduquest.api.dto.response.ReportResponseDTO;
import org.springframework.data.domain.Pageable;

public interface ReportService {
    ReportResponseDTO createReport(ReportRequestDTO request, String username);
    PageResponseDTO<ReportResponseDTO> getReports(String status, Pageable pageable);
    long countOpenReports();
    ReportResponseDTO updateStatus(Long reportId, String status, String moderatorUsername);
}
