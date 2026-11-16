package com.eduquest.api.service.impl;

import com.eduquest.api.dto.response.StickerResponseDTO;
import com.eduquest.api.entity.User;
import com.eduquest.api.entity.UserSticker;
import com.eduquest.api.exception.DuplicateResourceException;
import com.eduquest.api.exception.InvalidOperationException;
import com.eduquest.api.exception.ResourceNotFoundException;
import com.eduquest.api.repository.UserRepository;
import com.eduquest.api.repository.UserStickerRepository;
import com.eduquest.api.service.StickerService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class StickerServiceImpl implements StickerService {

    static final int MAX_STICKERS = 30;

    private final UserStickerRepository stickerRepository;
    private final UserRepository userRepository;

    public StickerServiceImpl(UserStickerRepository stickerRepository, UserRepository userRepository) {
        this.stickerRepository = stickerRepository;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<StickerResponseDTO> getMyStickers(String username) {
        User user = findUser(username);
        return stickerRepository.findByUserIdOrderByCreatedAtDesc(user.getId()).stream()
                .map(StickerServiceImpl::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public StickerResponseDTO addSticker(String imageUrl, String username) {
        User user = findUser(username);
        String url = imageUrl.trim();
        if (stickerRepository.existsByUserIdAndImageUrl(user.getId(), url)) {
            throw new DuplicateResourceException("Ese sticker ya está en tu colección");
        }
        if (stickerRepository.countByUserId(user.getId()) >= MAX_STICKERS) {
            throw new InvalidOperationException("Puedes guardar hasta " + MAX_STICKERS + " stickers");
        }
        UserSticker sticker = new UserSticker();
        sticker.setUser(user);
        sticker.setImageUrl(url);
        sticker.setCreatedAt(LocalDateTime.now());
        return toResponse(stickerRepository.save(sticker));
    }

    @Override
    @Transactional
    public void deleteSticker(Long stickerId, String username) {
        User user = findUser(username);
        UserSticker sticker = stickerRepository.findByIdAndUserId(stickerId, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Sticker no encontrado"));
        stickerRepository.delete(sticker);
    }

    private User findUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
    }

    private static StickerResponseDTO toResponse(UserSticker sticker) {
        return new StickerResponseDTO(sticker.getId(), sticker.getImageUrl());
    }
}
