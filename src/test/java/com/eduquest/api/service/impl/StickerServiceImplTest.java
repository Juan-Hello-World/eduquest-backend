package com.eduquest.api.service.impl;

import com.eduquest.api.dto.response.StickerResponseDTO;
import com.eduquest.api.entity.User;
import com.eduquest.api.entity.UserSticker;
import com.eduquest.api.exception.DuplicateResourceException;
import com.eduquest.api.exception.InvalidOperationException;
import com.eduquest.api.exception.ResourceNotFoundException;
import com.eduquest.api.repository.UserRepository;
import com.eduquest.api.repository.UserStickerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class StickerServiceImplTest {

    private static final String IMAGE = "http://localhost:8080/api/v1/files/0f8c6a43-6a1e-4c55-9a6f-9a7d5a1b2c3d/gato.png";

    @Mock UserStickerRepository stickerRepository;
    @Mock UserRepository userRepository;

    @InjectMocks StickerServiceImpl stickerService;

    private User alice;

    @BeforeEach
    void setUp() {
        alice = new User();
        alice.setId(1L);
        alice.setUsername("alice");
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(alice));
        when(stickerRepository.save(any(UserSticker.class))).thenAnswer(invocation -> {
            UserSticker sticker = invocation.getArgument(0);
            sticker.setId(5L);
            return sticker;
        });
    }

    @Test
    void addSticker_savesTrimmedUrl() {
        StickerResponseDTO dto = stickerService.addSticker("  " + IMAGE + " ", "alice");

        assertThat(dto.id()).isEqualTo(5L);
        assertThat(dto.imageUrl()).isEqualTo(IMAGE);
    }

    @Test
    void addSticker_acceptsAnimatedGif() {
        String gif = "http://localhost:8080/api/v1/files/0f8c6a43-6a1e-4c55-9a6f-9a7d5a1b2c3d/baile.gif";

        assertThat(stickerService.addSticker(gif, "alice").imageUrl()).endsWith("baile.gif");
        assertThat(gif).matches(java.util.regex.Pattern.compile(
                com.eduquest.api.dto.request.ChatMessages.STICKER_IMAGE_PATTERN, java.util.regex.Pattern.CASE_INSENSITIVE));
        assertThat("pack:bravo").matches(com.eduquest.api.dto.request.ChatMessages.STICKER_PATTERN);
    }

    @Test
    void addSticker_rejectsDuplicatesAndFullCollections() {
        when(stickerRepository.existsByUserIdAndImageUrl(1L, IMAGE)).thenReturn(true);
        assertThatThrownBy(() -> stickerService.addSticker(IMAGE, "alice")).isInstanceOf(DuplicateResourceException.class);

        when(stickerRepository.existsByUserIdAndImageUrl(1L, IMAGE)).thenReturn(false);
        when(stickerRepository.countByUserId(1L)).thenReturn((long) StickerServiceImpl.MAX_STICKERS);
        assertThatThrownBy(() -> stickerService.addSticker(IMAGE, "alice")).isInstanceOf(InvalidOperationException.class);
    }

    @Test
    void getAndDeleteStickers() {
        UserSticker sticker = new UserSticker();
        sticker.setId(5L);
        sticker.setImageUrl(IMAGE);
        when(stickerRepository.findByUserIdOrderByCreatedAtDesc(1L)).thenReturn(List.of(sticker));
        when(stickerRepository.findByIdAndUserId(5L, 1L)).thenReturn(Optional.of(sticker));

        assertThat(stickerService.getMyStickers("alice")).extracting(StickerResponseDTO::imageUrl).containsExactly(IMAGE);
        stickerService.deleteSticker(5L, "alice");
        verify(stickerRepository).delete(sticker);
        assertThatThrownBy(() -> stickerService.deleteSticker(6L, "alice")).isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> stickerService.getMyStickers("ghost")).isInstanceOf(ResourceNotFoundException.class);
    }
}
