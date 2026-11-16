package com.eduquest.api.repository;

import com.eduquest.api.entity.User;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    String NOT_BANNED = "(u.banned IS NULL OR u.banned = false OR u.bannedUntil < CURRENT_TIMESTAMP)";
    Optional<User> findByUsername(String username);
    Boolean existsByUsername(String username);
    Boolean existsByEmail(String email);

    Optional<User> findByEmail(String email);

    Optional<User> findByEmailIgnoreCase(String email);

    @Query("SELECT u FROM User u WHERE LOWER(u.username) LIKE :pattern AND u.id <> :excludedId "
            + "AND " + NOT_BANNED + " ORDER BY u.username ASC")
    List<User> searchActive(@Param("pattern") String pattern, @Param("excludedId") Long excludedId, Pageable pageable);

    @Query("SELECT u FROM User u WHERE " + NOT_BANNED)
    List<User> findActive();

    @Query("SELECT COUNT(u) FROM User u JOIN u.following f WHERE f.id = :userId")
    long countFollowers(@Param("userId") Long userId);

    @Query("SELECT COUNT(f) FROM User u JOIN u.following f WHERE u.id = :userId")
    long countFollowing(@Param("userId") Long userId);

    @Query("SELECT u FROM User u JOIN u.following f WHERE f.id = :userId")
    List<User> findFollowers(@Param("userId") Long userId);

    @Query("SELECT f FROM User u JOIN u.following f WHERE u.id = :userId ORDER BY f.username")
    List<User> findFollowing(@Param("userId") Long userId);

    boolean existsByIdAndFollowingId(Long followerId, Long followedId);

    long countByCreatedAtAfter(java.time.LocalDateTime after);

    @Query("SELECT COUNT(u) FROM User u WHERE u.banned = true AND (u.bannedUntil IS NULL OR u.bannedUntil > CURRENT_TIMESTAMP)")
    long countSuspended();
}