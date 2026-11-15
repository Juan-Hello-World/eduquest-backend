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
    Optional<User> findByUsername(String username);
    Boolean existsByUsername(String username);
    Boolean existsByEmail(String email);

    Optional<User> findByEmail(String email);

    Optional<User> findByEmailIgnoreCase(String email);

    @Query("SELECT u FROM User u WHERE LOWER(u.username) LIKE :pattern AND u.id <> :excludedId "
            + "AND (u.banned IS NULL OR u.banned = false) ORDER BY u.username ASC")
    List<User> searchActive(@Param("pattern") String pattern, @Param("excludedId") Long excludedId, Pageable pageable);

    @Query("SELECT u FROM User u WHERE u.banned IS NULL OR u.banned = false")
    List<User> findActive();
}