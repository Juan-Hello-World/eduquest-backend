package com.eduquest.api.security;

import com.eduquest.api.entity.Role;
import com.eduquest.api.entity.RoleName;
import com.eduquest.api.entity.User;
import com.eduquest.api.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class UserDetailsServiceImplTest {

    @Mock UserRepository userRepository;

    @InjectMocks UserDetailsServiceImpl userDetailsService;

    @Test
    void loadUserByUsername_withExistingUser_buildsPrincipal() {
        User user = new User();
        user.setId(1L);
        user.setUsername("alice");
        user.setEmail("alice@utec.edu.pe");
        user.setPassword("hash");
        user.setRoles(Set.of(new Role(1L, RoleName.ROLE_USER)));
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));

        UserPrincipal principal = (UserPrincipal) userDetailsService.loadUserByUsername("alice");

        assertThat(principal.getUsername()).isEqualTo("alice");
        assertThat(principal.getAuthorities()).hasSize(1);
    }

    @Test
    void loadUserByUsername_withUnknownUser_throwsUsernameNotFound() {
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userDetailsService.loadUserByUsername("ghost"))
                .isInstanceOf(UsernameNotFoundException.class);
    }
}