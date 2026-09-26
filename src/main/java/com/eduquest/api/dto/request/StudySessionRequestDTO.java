package com.eduquest.api.dto.request;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class StudySessionRequestDTO {
    @NotBlank
    private String title;

    private String description;

    @NotNull
    @Future
    private LocalDateTime scheduledAt;

    private String meetingUrl;


}