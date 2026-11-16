package com.eduquest.api.repository;

import com.eduquest.api.entity.PushToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface PushTokenRepository extends JpaRepository<PushToken, Long> {

    Optional<PushToken> findByToken(String token);

    List<PushToken> findByUserIdIn(Collection<Long> userIds);

    @Modifying
    @Query("DELETE FROM PushToken t WHERE t.token IN :tokens")
    void deleteByTokenIn(@Param("tokens") Collection<String> tokens);

    @Modifying
    @Query("DELETE FROM PushToken t WHERE t.token = :token AND t.user.id = :userId")
    void deleteByTokenAndUserId(@Param("token") String token, @Param("userId") Long userId);
}
