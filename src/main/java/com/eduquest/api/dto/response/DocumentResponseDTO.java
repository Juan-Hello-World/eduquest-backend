package com.eduquest.api.dto.response;

import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter
@Setter
public class DocumentResponseDTO {
    private Long id;
    private String title;
    private String description;
    private String fileUrl;
    private LocalDateTime uploadedAt;
    private String authorUsername;
    private String groupName;
}