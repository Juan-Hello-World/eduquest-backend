package com.eduquest.api.dto.response;

public record FileUploadResponseDTO(String url, String fileName, String contentType, long size) {
}
