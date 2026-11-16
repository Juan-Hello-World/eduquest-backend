package com.eduquest.api.repository;

import com.eduquest.api.entity.AppealStatus;
import com.eduquest.api.entity.BanAppeal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BanAppealRepository extends JpaRepository<BanAppeal, Long> {

    boolean existsByUserIdAndStatus(Long userId, AppealStatus status);

    Page<BanAppeal> findByStatusOrderByCreatedAtDesc(AppealStatus status, Pageable pageable);

    Page<BanAppeal> findAllByOrderByCreatedAtDesc(Pageable pageable);

    long countByStatus(AppealStatus status);
}
