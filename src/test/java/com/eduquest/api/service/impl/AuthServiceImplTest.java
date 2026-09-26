package com.eduquest.api.service.impl;

import com.eduquest.api.dto.request.LoginRequestDTO;
import com.eduquest.api.dto.request.RegisterRequestDTO;
import com.eduquest.api.dto.response.AuthResponseDTO;
import com.eduquest.api.entity.Role;
import com.eduquest.api.entity.RoleName;
import com.eduquest.api.entity.User;
import com.eduquest.api.exception.DuplicateResourceException;
import com.eduquest.api.exception.InvalidCredentialsException;
import com.eduquest.api.exception.UnauthorizedException;
import com.eduquest.api.repository.RoleRepository;
import com.eduquest.api.repository.UserRepository;
import com.eduquest.api.security.JwtService;
import com.eduquest.api.security.UserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AuthServiceImplTest {

    @Mock AuthenticationManager authenticationManager;
    @Mock UserRepository userRepository;
    @Mock RoleRepository roleRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock JwtService jwtService;
    @Mock ApplicationEventPublisher eventPublisher;

    @InjectMocks AuthServiceImpl authService;

    private User user;
    private Role role;
    private UserPrincipal principal;

    @BeforeEach
    void setUp() {
        role = new Role(1L, RoleName.ROLE_USER);
        user = new User();
        user.setId(1L);
        user.setUsername("alice");
        user.setEmail("alice@utec.edu.pe");
        user.setPassword("hash");
        user.setRoles(Set.of(role));
        principal = UserPrincipal.build(user);

        when(jwtService.generateAccessToken(any(UserPrincipal.class))).thenReturn("access-token");
        when(jwtService.generateRefreshToken(any(UserPrincipal.class))).thenReturn("refresh-token");
    }

    @Test
    void authenticateUser_onValidCredentials_returnsTokensAndRoles() {
        Authentication authentication = org.mockito.Mockito.mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(principal);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));

        LoginRequestDTO request = new LoginRequestDTO();
        request.setUsername("alice");
        request.setPassword("Password123");

        AuthResponseDTO response = authService.authenticateUser(request);

        assertThat(response.getToken()).isEqualTo("access-token");
        assertThat(response.getRefreshToken()).isEqualTo("refresh-token");
        assertThat(response.getUsername()).isEqualTo("alice");
        assertThat(response.getRoles()).contains("ROLE_USER");
    }

    @Test
    void authenticateUser_onBadCredentials_throwsInvalidCredentials() {
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("bad"));

        LoginRequestDTO request = new LoginRequestDTO();
        request.setUsername("alice");
        request.setPassword("Wrong123");

        assertThatThrownBy(() -> authService.authenticateUser(request))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("Usuario o contraseña incorrectos");
    }

    @Test
    void registerUser_onNewUser_persistsAndReturnsTokens() {
        when(userRepository.existsByUsername("bob")).thenReturn(false);
        when(userRepository.existsByEmail("bob@utec.edu.pe")).thenReturn(false);
        when(roleRepository.findByName(RoleName.ROLE_USER)).thenReturn(Optional.of(role));
        when(passwordEncoder.encode(anyString())).thenReturn("bcrypt-hash");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RegisterRequestDTO request = new RegisterRequestDTO();
        request.setUsername("bob");
        request.setEmail("bob@utec.edu.pe");
        request.setPassword("Password123");

        AuthResponseDTO response = authService.registerUser(request);

        assertThat(response.getUsername()).isEqualTo("bob");
        assertThat(response.getRoles()).contains("ROLE_USER");
        verify(eventPublisher).publishEvent(any());
    }

    @Test
    void registerUser_onDuplicateUsername_throwsConflict() {
        when(userRepository.existsByUsername("bob")).thenReturn(true);

        RegisterRequestDTO request = new RegisterRequestDTO();
        request.setUsername("bob");
        request.setEmail("bob@utec.edu.pe");
        request.setPassword("Password123");

        assertThatThrownBy(() -> authService.registerUser(request))
                .isInstanceOf(DuplicateResourceException.class);
    }

    @Test
    void registerUser_onDuplicateEmail_throwsConflict() {
        when(userRepository.existsByEmail("bob@utec.edu.pe")).thenReturn(true);

        RegisterRequestDTO request = new RegisterRequestDTO();
        request.setUsername("bob");
        request.setEmail("bob@utec.edu.pe");
        request.setPassword("Password123");

        assertThatThrownBy(() -> authService.registerUser(request))
                .isInstanceOf(DuplicateResourceException.class);
    }

    @Test
    void refreshToken_onValidRefreshToken_returnsNewTokens() {
        when(jwtService.isValidToken("rt")).thenReturn(true);
        when(jwtService.getTokenType("rt")).thenReturn("refresh");
        when(jwtService.extractUsername("rt")).thenReturn("alice");
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));

        AuthResponseDTO response = authService.refreshToken("rt");

        assertThat(response.getToken()).isEqualTo("access-token");
        assertThat(response.getUsername()).isEqualTo("alice");
    }

    @Test
    void refreshToken_onInvalidOrWrongType_throwsUnauthorized() {
        when(jwtService.isValidToken("bad")).thenReturn(false);

        assertThatThrownBy(() -> authService.refreshToken("bad"))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void refreshToken_onUnknownUser_throwsUnauthorized() {
        when(jwtService.isValidToken("rt")).thenReturn(true);
        when(jwtService.getTokenType("rt")).thenReturn("refresh");
        when(jwtService.extractUsername("rt")).thenReturn("ghost");
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.refreshToken("rt"))
                .isInstanceOf(UnauthorizedException.class);
    }
}