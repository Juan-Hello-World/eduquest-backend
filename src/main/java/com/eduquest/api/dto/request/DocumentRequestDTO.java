package com.eduquest.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import java.util.Set;

@Getter
@Setter
public class DocumentRequestDTO {
    @NotBlank
    @Size(max = 150)
    private String title;

    private String description;

    @NotBlank
    @Pattern(regexp="^https?://.+", message = "La URL del archivo debe comenzar con http:// o https://")
    private String fileUrl;

    private Set<String> tags;
}