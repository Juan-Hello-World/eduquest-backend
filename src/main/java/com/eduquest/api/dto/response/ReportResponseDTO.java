package com.eduquest.api.dto.response;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class ReportResponseDTO {
    private Long id;
    private String targetType;
    private Long targetId;
    private String targetLabel;
    private String reason;
    private String status;
    private String reporterUsername;
    private LocalDateTime createdAt;
    private LocalDateTime resolvedAt;
    private String resolvedByUsername;
}
