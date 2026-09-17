package com.eduquest.api.dto.response;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class DocumentResponseDTO {
    private Long id;
    private String title;
    private String description;
    private String fileUrl;
    private LocalDateTime uploadedAt;
    private String authorUsername;
}