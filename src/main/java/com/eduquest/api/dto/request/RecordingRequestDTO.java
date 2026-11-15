package com.eduquest.api.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RecordingRequestDTO {
    @Size(max = 255, message = "El enlace es demasiado largo")
    private String recordingUrl;
}
