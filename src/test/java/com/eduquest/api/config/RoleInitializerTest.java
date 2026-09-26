package com.eduquest.api.config;

import com.eduquest.api.entity.Role;
import com.eduquest.api.entity.RoleName;
import com.eduquest.api.entity.User;
import com.eduquest.api.repository.RoleRepository;
import com.eduquest.api.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RoleInitializerTest {

    @Mock RoleRepository roleRepository;
    @Mock UserRepository userRepository;
    @Mock PasswordEncoder passwordEncoder;

    @InjectMocks RoleInitializer roleInitializer;

    @Test
    void run_seedsMissingRolesAndCreatesAdmin() {
        Role userRole = new Role(1L, RoleName.ROLE_USER);
        Role adminRole = new Role(2L, RoleName.ROLE_ADMIN);
        when(roleRepository.findByName(RoleName.ROLE_USER)).thenReturn(Optional.empty());
        when(roleRepository.findByName(RoleName.ROLE_ADMIN)).thenReturn(Optional.of(adminRole));
        when(roleRepository.findByName(RoleName.ROLE_MANAGER)).thenReturn(Optional.empty());
        when(roleRepository.save(any(Role.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(userRepository.existsByUsername("admin")).thenReturn(false);
        when(passwordEncoder.encode("admin123")).thenReturn("encoded");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        roleInitializer.run("non-blank-arg");

        verify(roleRepository, times(2)).save(any(Role.class));
        verify(userRepository).save(any(User.class));
    }

    @Test
    void run_whenAllRolesAndAdminExist_doesNothing() {
        Role userRole = new Role(1L, RoleName.ROLE_USER);
        Role adminRole = new Role(2L, RoleName.ROLE_ADMIN);
        Role managerRole = new Role(3L, RoleName.ROLE_MANAGER);
        when(roleRepository.findByName(RoleName.ROLE_USER)).thenReturn(Optional.of(userRole));
        when(roleRepository.findByName(RoleName.ROLE_ADMIN)).thenReturn(Optional.of(adminRole));
        when(roleRepository.findByName(RoleName.ROLE_MANAGER)).thenReturn(Optional.of(managerRole));
        when(userRepository.existsByUsername("admin")).thenReturn(true);

        roleInitializer.run("non-blank-arg");

        verify(roleRepository, never()).save(any(Role.class));
        verify(userRepository, never()).save(any(User.class));
        assertThat(managerRole.getName()).isEqualTo(RoleName.ROLE_MANAGER);
    }
}