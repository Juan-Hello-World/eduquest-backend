package com.eduquest.api.dto.response;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class SessionReviewResponseDTO {
    private Long id;
    private Integer rating;
    private String comment;
    private String authorUsername;
    private String authorAvatarUrl;
    private LocalDateTime createdAt;
}
