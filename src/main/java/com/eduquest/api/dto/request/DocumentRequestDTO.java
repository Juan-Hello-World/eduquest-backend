package com.eduquest.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DocumentRequestDTO {
    @NotBlank
    @Size(max = 150)
    private String title;

    private String description;

    @NotBlank
    private String fileUrl;
}