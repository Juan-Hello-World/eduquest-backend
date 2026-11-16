package com.eduquest.api.service;

import com.eduquest.api.dto.response.StickerResponseDTO;

import java.util.List;

public interface StickerService {
    List<StickerResponseDTO> getMyStickers(String username);
    StickerResponseDTO addSticker(String imageUrl, String username);
    void deleteSticker(Long stickerId, String username);
}
