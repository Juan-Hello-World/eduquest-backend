package com.eduquest.api.dto.request;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GroupMessageRequestDTO {
    @Size(max = 1000, message = "El mensaje no puede superar los 1000 caracteres")
    private String content;

    @Size(max = 500)
    @Pattern(regexp = ChatMessages.IMAGE_URL_PATTERN, flags = Pattern.Flag.CASE_INSENSITIVE,
            message = "La imagen debe subirse primero a EduQuest")
    private String imageUrl;

    @Size(max = 500)
    @Pattern(regexp = ChatMessages.FILE_URL_PATTERN, flags = Pattern.Flag.CASE_INSENSITIVE,
            message = "El archivo debe subirse primero a EduQuest")
    private String fileUrl;

    @Size(max = 200)
    private String fileName;

    @Size(max = 500)
    @Pattern(regexp = ChatMessages.STICKER_PATTERN, flags = Pattern.Flag.CASE_INSENSITIVE, message = "Sticker no válido")
    private String sticker;

    private Long replyToId;

    private Long channelId;

    private Long threadRootId;
}
