package com.eduquest.api.security;

import com.eduquest.api.entity.Role;
import com.eduquest.api.entity.RoleName;
import com.eduquest.api.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;

import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class UserPrincipalTest {

    @Test
    void build_mapsRolesToAuthorities() {
        User user = new User();
        user.setId(1L);
        user.setUsername("alice");
        user.setEmail("alice@utec.edu.pe");
        user.setPassword("hash");
        user.setRoles(Set.of(new Role(1L, RoleName.ROLE_USER), new Role(2L, RoleName.ROLE_ADMIN)));

        UserPrincipal principal = UserPrincipal.build(user);

        assertThat(principal.getId()).isEqualTo(1L);
        assertThat(principal.getUsername()).isEqualTo("alice");
        assertThat(principal.getEmail()).isEqualTo("alice@utec.edu.pe");
        assertThat(principal.getPassword()).isEqualTo("hash");
        Set<String> authorities = principal.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());
        assertThat(authorities).containsExactlyInAnyOrder("ROLE_USER", "ROLE_ADMIN");
    }
}