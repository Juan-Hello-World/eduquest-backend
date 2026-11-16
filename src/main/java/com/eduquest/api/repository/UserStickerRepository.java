package com.eduquest.api.repository;

import com.eduquest.api.entity.UserSticker;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserStickerRepository extends JpaRepository<UserSticker, Long> {

    List<UserSticker> findByUserIdOrderByCreatedAtDesc(Long userId);

    long countByUserId(Long userId);

    Optional<UserSticker> findByIdAndUserId(Long id, Long userId);

    boolean existsByUserIdAndImageUrl(Long userId, String imageUrl);
}
