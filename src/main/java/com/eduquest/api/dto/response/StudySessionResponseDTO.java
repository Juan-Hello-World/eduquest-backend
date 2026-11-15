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
    private String createdByUsername;
    private boolean canManage;
    private boolean createdByAdvisor;
    private boolean finished;
    private int attendeeCount;
    private boolean attending;
    private Double averageRating;
    private int reviewCount;
    private boolean reviewedByMe;
    private boolean canReview;
    private String recordingUrl;
    private String recordingTitle;
    private String recordingVideoId;
}