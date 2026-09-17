package com.eduquest.api.dto.response;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class StudySessionResponseDTO {
    private Long id;
    private String title;
    private String description;
    private LocalDateTime scheduledAt;
    private String meetingUrl;
    private String groupName;
}