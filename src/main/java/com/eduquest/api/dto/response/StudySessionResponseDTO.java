package com.eduquest.api.dto.response;

import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter
@Setter
public class StudySessionResponseDTO {
    private Long id;
    private String title;
    private String description;
    private LocalDateTime scheduledAt;
    private String meetingUrl;
    private String groupName;
}