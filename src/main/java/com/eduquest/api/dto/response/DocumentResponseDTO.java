package com.eduquest.api.dto.response;

import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;
import java.util.Set;

@Getter
@Setter
public class DocumentResponseDTO {
    private Long id;
    private String title;
    private String description;
    private String fileUrl;
    private LocalDateTime uploadedAt;
    private Long authorId;
    private String authorUsername;
    private String groupName;
    private String course;
    private String university;
    private boolean featured;
    private Set<String> tags;
}