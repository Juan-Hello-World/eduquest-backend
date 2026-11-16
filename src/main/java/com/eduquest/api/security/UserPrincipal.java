package com.eduquest.api.security;

import com.eduquest.api.entity.Role;
import com.eduquest.api.entity.User;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

@Getter
public class UserPrincipal implements UserDetails {

    private final Long id;
    private final String username;
    private final String email;
    private final String password;
    private final Collection<? extends GrantedAuthority> authorities;
    private final boolean banned;
    private final String banReason;
    private final LocalDateTime bannedAt;
    private final LocalDateTime bannedUntil;
    private boolean emailVerified = true;

    public UserPrincipal(Long id, String username, String email, String password,
                         Collection<? extends GrantedAuthority> authorities) {
        this(id, username, email, password, authorities, false, null, null);
    }

    public UserPrincipal(Long id, String username, String email, String password,
                         Collection<? extends GrantedAuthority> authorities,
                         boolean banned, String banReason, LocalDateTime bannedAt) {
        this(id, username, email, password, authorities, banned, banReason, bannedAt, null);
    }

    public UserPrincipal(Long id, String username, String email, String password,
                         Collection<? extends GrantedAuthority> authorities,
                         boolean banned, String banReason, LocalDateTime bannedAt, LocalDateTime bannedUntil) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.password = password;
        this.authorities = authorities;
        this.banned = banned;
        this.banReason = banReason;
        this.bannedAt = bannedAt;
        this.bannedUntil = bannedUntil;
    }

    public static UserPrincipal build(User user) {
        Set<GrantedAuthority> authorities = user.getRoles().stream()
                .map(Role::getName)
                .map(name -> new SimpleGrantedAuthority(name.name()))
                .collect(Collectors.toSet());

        UserPrincipal principal = new UserPrincipal(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getPassword(),
                authorities,
                user.isBanned(),
                user.getBanReason(),
                user.getBannedAt(),
                user.getBannedUntil()
        );
        principal.emailVerified = user.isEmailVerified();
        return principal;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
    }
}