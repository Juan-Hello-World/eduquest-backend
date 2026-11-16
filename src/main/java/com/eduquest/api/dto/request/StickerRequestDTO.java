package com.eduquest.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class StickerRequestDTO {
    @NotBlank(message = "Sube una imagen para el sticker")
    @Size(max = 500)
    @Pattern(regexp = ChatMessages.STICKER_IMAGE_PATTERN, flags = Pattern.Flag.CASE_INSENSITIVE,
            message = "La imagen debe subirse primero a EduQuest")
    private String imageUrl;
}
