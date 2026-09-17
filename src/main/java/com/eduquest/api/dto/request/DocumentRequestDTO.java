package com.eduquest.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class DocumentRequestDTO {
    @NotBlank
    @Size(max = 150)
    private String title;

    private String description;

    @NotBlank
    private String fileUrl;
}