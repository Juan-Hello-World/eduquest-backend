package com.eduquest.api.dto.response;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class GroupMessageResponseDTO {
    private Long id;
    private String content;
    private Long authorId;
    private String authorUsername;
    private String authorAvatarUrl;
    private boolean authorAdvisor;
    private boolean mine;
    private LocalDateTime createdAt;
}
