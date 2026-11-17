package com.eduquest.api.dto.response;

public record AvatarImageDTO(byte[] data, String contentType, String redirectUrl) {
}
