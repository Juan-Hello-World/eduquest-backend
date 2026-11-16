package com.eduquest.api.service.impl;

import com.eduquest.api.dto.request.LoginRequestDTO;
import com.eduquest.api.dto.request.RegisterRequestDTO;
import com.eduquest.api.dto.response.AuthResponseDTO;
import com.eduquest.api.entity.Role;
import com.eduquest.api.entity.RoleName;
import com.eduquest.api.entity.User;
import com.eduquest.api.event.OnUserRegistrationEvent;
import com.eduquest.api.exception.DuplicateResourceException;
import com.eduquest.api.exception.InvalidCredentialsException;
import com.eduquest.api.exception.ResourceNotFoundException;
import com.eduquest.api.exception.UnauthorizedException;
import com.eduquest.api.exception.UserBannedException;
import com.eduquest.api.repository.RoleRepository;
import com.eduquest.api.repository.UserRepository;
import com.eduquest.api.security.JwtService;
import com.eduquest.api.security.LoginAttemptService;
import com.eduquest.api.security.UserPrincipal;
import com.eduquest.api.service.AuthService;
import com.eduquest.api.service.EmailVerificationService;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AuthServiceImpl implements AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final ApplicationEventPublisher eventPublisher;
    private final LoginAttemptService loginAttemptService;
    private final EmailVerificationService emailVerificationService;

    public AuthServiceImpl(AuthenticationManager authenticationManager, UserRepository userRepository,
                           RoleRepository roleRepository, PasswordEncoder passwordEncoder,
                           JwtService jwtService, ApplicationEventPublisher eventPublisher,
                           LoginAttemptService loginAttemptService,
                           EmailVerificationService emailVerificationService) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.eventPublisher = eventPublisher;
        this.loginAttemptService = loginAttemptService;
        this.emailVerificationService = emailVerificationService;
    }

    private AuthResponseDTO buildAuthResponse(UserPrincipal principal, Set<String> roles) {
        String accessToken = jwtService.generateAccessToken(principal);
        String refreshToken = jwtService.generateRefreshToken(principal);
        AuthResponseDTO response = new AuthResponseDTO(accessToken, refreshToken, principal.getId(),
                principal.getUsername(), principal.getEmail(), roles);
        response.setEmailVerified(principal.isEmailVerified());
        return response;
    }

    private void ensureNotBanned(User user) {
        if (user.isBanned()) {
            throw new UserBannedException(user.getBanReason(), user.getBannedAt(), user.getBannedUntil());
        }
    }

    private Set<String> extractRoleNames(User user) {
        return user.getRoles().stream()
                .map(role -> role.getName().name())
                .collect(Collectors.toSet());
    }

    @Override
    public AuthResponseDTO authenticateUser(LoginRequestDTO loginRequest) {
        String username = loginRequest.getUsername();
        loginAttemptService.ensureAllowed(username);
        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(username, loginRequest.getPassword())
            );
        } catch (BadCredentialsException ex) {
            loginAttemptService.recordFailure(username);
            int remaining = loginAttemptService.remainingAttempts(username);
            if (remaining == 0) {
                loginAttemptService.ensureAllowed(username);
            }
            throw new InvalidCredentialsException(remaining <= 2
                    ? "Usuario o contraseña incorrectos. Te " + (remaining == 1 ? "queda 1 intento" : "quedan " + remaining + " intentos")
                    : "Usuario o contraseña incorrectos");
        }
        loginAttemptService.recordSuccess(username);

        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        User user = userRepository.findByUsername(principal.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
        ensureNotBanned(user);

        SecurityContextHolder.getContext().setAuthentication(authentication);

        return buildAuthResponse(principal, extractRoleNames(user));
    }

    @Override
    @Transactional
    public AuthResponseDTO registerUser(RegisterRequestDTO registerRequest) {
        userRepository.findByEmail(registerRequest.getEmail()).ifPresent(this::ensureNotBanned);
        userRepository.findByUsername(registerRequest.getUsername()).ifPresent(this::ensureNotBanned);

        if (userRepository.existsByUsername(registerRequest.getUsername())) {
            throw new DuplicateResourceException("El nombre de usuario ya está en uso");
        }

        if (userRepository.existsByEmail(registerRequest.getEmail())) {
            throw new DuplicateResourceException("El correo electrónico ya está registrado");
        }

        User user = new User();
        user.setUsername(registerRequest.getUsername());
        user.setEmail(registerRequest.getEmail());
        user.setPassword(passwordEncoder.encode(registerRequest.getPassword()));
        user.setAdvisor(Boolean.TRUE.equals(registerRequest.getAdvisor()));
        user.setCreatedAt(LocalDateTime.now());
        user.setEmailVerified(!emailVerificationService.isEnabled());

        Role userRole = roleRepository.findByName(RoleName.ROLE_USER)
                .orElseThrow(() -> new ResourceNotFoundException("Rol de usuario no establecido en la base de datos"));

        user.setRoles(Collections.singleton(userRole));
        User savedUser = userRepository.save(user);

        UserPrincipal principal = UserPrincipal.build(savedUser);
        Set<String> roles = extractRoleNames(savedUser);

        // Evento asíncrono de correo de bienvenida (se dispara tras el commit)
        eventPublisher.publishEvent(new OnUserRegistrationEvent(this, savedUser.getEmail(), savedUser.getUsername()));
        if (!savedUser.isEmailVerified()) {
            emailVerificationService.sendCode(savedUser);
        }

        return buildAuthResponse(principal, roles);
    }

    @Override
    @Transactional(readOnly = true)
    public AuthResponseDTO refreshToken(String refreshToken) {
        if (!jwtService.isValidToken(refreshToken)
                || !"refresh".equals(jwtService.getTokenType(refreshToken))) {
            throw new UnauthorizedException("Refresh token inválido o expirado");
        }

        String username = jwtService.extractUsername(refreshToken);
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UnauthorizedException("Usuario no encontrado"));
        ensureNotBanned(user);

        AuthResponseDTO response = buildAuthResponse(UserPrincipal.build(user), extractRoleNames(user));
        response.setRefreshToken(refreshToken);
        return response;
    }
}