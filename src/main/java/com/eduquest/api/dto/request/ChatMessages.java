package com.eduquest.api.dto.request;

import com.eduquest.api.exception.BadRequestException;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

public final class ChatMessages {

    public static final String IMAGE_URL_PATTERN = "^https?://\\S+/api/v1/files/[0-9a-f-]{36}/\\S+\\.(png|jpe?g|webp)$";
    public static final String FILE_URL_PATTERN = "^https?://\\S+/api/v1/files/[0-9a-f-]{36}/\\S+$";
    public static final String STICKER_IMAGE_PATTERN =
            "^https?://\\S+/api/v1/files/[0-9a-f-]{36}/\\S+\\.(png|jpe?g|webp|gif)$";
    public static final String STICKER_PATTERN =
            "^(pack:[a-z0-9-]{2,30}|https?://\\S+/api/v1/files/[0-9a-f-]{36}/\\S+\\.(png|jpe?g|webp|gif))$";

    private ChatMessages() {
    }

    public static String cleanContent(String content, String imageUrl) {
        return cleanContent(content, imageUrl != null && !imageUrl.isBlank());
    }

    public static String cleanContent(String content, boolean hasAttachment) {
        String clean = content == null ? "" : content.trim();
        if (clean.isEmpty() && !hasAttachment) {
            throw new BadRequestException("Escribe un mensaje o adjunta una imagen");
        }
        return clean;
    }

    public static boolean isVideo(String fileName) {
        return fileName != null && fileName.toLowerCase(Locale.ROOT).matches(".*\\.(mp4|webm|mov)$");
    }

    public static boolean present(String value) {
        return value != null && !value.isBlank();
    }

    public static String cleanFileName(String fileName, String fileUrl) {
        if (!present(fileUrl)) {
            return null;
        }
        if (present(fileName)) {
            String clean = fileName.trim();
            return clean.length() <= 200 ? clean : clean.substring(0, 200);
        }
        String last = fileUrl.substring(fileUrl.lastIndexOf('/') + 1);
        return URLDecoder.decode(last, StandardCharsets.UTF_8);
    }

    public static String cleanImage(String imageUrl) {
        return imageUrl == null || imageUrl.isBlank() ? null : imageUrl.trim();
    }

    public static final String DELETED_PREVIEW = "Mensaje eliminado";
    private static final int REPLY_PREVIEW_LENGTH = 90;

    public static String replyPreview(String content, String imageUrl, boolean deleted) {
        return replyPreview(content, imageUrl, null, null, deleted);
    }

    public static String replyPreview(String content, String imageUrl, String fileName, String sticker, boolean deleted) {
        if (deleted) {
            return DELETED_PREVIEW;
        }
        String text = preview(content, imageUrl, fileName, sticker);
        return text.length() <= REPLY_PREVIEW_LENGTH ? text : text.substring(0, REPLY_PREVIEW_LENGTH - 1) + "…";
    }

    public static String preview(String content, String imageUrl) {
        return preview(content, imageUrl, null, null);
    }

    public static String preview(String content, String imageUrl, String fileName, String sticker) {
        if (content != null && !content.isBlank()) {
            return content;
        }
        if (present(sticker)) {
            return "Envió un sticker";
        }
        if (present(fileName)) {
            return isVideo(fileName) ? "Envió un video" : "Envió un archivo: " + fileName;
        }
        return imageUrl != null ? "Envió una imagen" : "";
    }
}
