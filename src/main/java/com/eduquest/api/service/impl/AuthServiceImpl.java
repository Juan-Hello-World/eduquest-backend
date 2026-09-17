package com.eduquest.api.service.impl;

import com.eduquest.api.dto.request.LoginRequestDTO;
import com.eduquest.api.dto.request.RegisterRequestDTO;
import com.eduquest.api.dto.response.AuthResponseDTO;
import com.eduquest.api.entity.Role;
import com.eduquest.api.entity.User;
import com.eduquest.api.exception.DuplicateResourceException;
import com.eduquest.api.exception.ResourceNotFoundException;
import com.eduquest.api.repository.RoleRepository;
import com.eduquest.api.repository.UserRepository;
import com.eduquest.api.security.JwtTokenProvider;
import com.eduquest.api.service.AuthService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.context.ApplicationEventPublisher;
import com.eduquest.api.event.OnUserRegistrationEvent;


import java.util.Collections;

@Service
public class AuthServiceImpl implements AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;
    private final ApplicationEventPublisher eventPublisher;



    public AuthServiceImpl(AuthenticationManager authenticationManager, UserRepository userRepository,
                           RoleRepository roleRepository, PasswordEncoder passwordEncoder,
                           JwtTokenProvider tokenProvider, ApplicationEventPublisher eventPublisher) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public AuthResponseDTO authenticateUser(LoginRequestDTO loginRequest) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword())
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);
        String jwt = tokenProvider.generateToken(authentication);

        User user = userRepository.findByUsername(loginRequest.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        return new AuthResponseDTO(jwt, user.getId(), user.getUsername(), user.getEmail());
    }

    @Override
    @Transactional
    public AuthResponseDTO registerUser(RegisterRequestDTO registerRequest) {
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

        Role userRole = roleRepository.findByName(Role.ERole.ROLE_USER)
                .orElseThrow(() -> new ResourceNotFoundException("Rol de usuario no establecido en la base de datos"));

        user.setRoles(Collections.singleton(userRole));
        User savedUser = userRepository.save(user);

        // Auto-login después de registrarse
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(registerRequest.getUsername(), registerRequest.getPassword())
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);
        String jwt = tokenProvider.generateToken(authentication);
        // Disparar evento asíncrono de correo
        eventPublisher.publishEvent(new OnUserRegistrationEvent(this, savedUser.getEmail(), savedUser.getUsername()));

        return new AuthResponseDTO(jwt, savedUser.getId(), savedUser.getUsername(), savedUser.getEmail());
    }
}