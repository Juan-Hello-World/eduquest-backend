package com.eduquest.api.dto.response;

public record StoredFileDTO(byte[] data, String contentType, String fileName) {
}
