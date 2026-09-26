package com.eduquest.api.controller;

import com.eduquest.api.dto.response.UserResponseDTO;
import com.eduquest.api.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/users")
@Tag(name = "Usuarios", description = "Información del usuario autenticado.")
public class UserController {

    @GetMapping("/me")
    @Operation(summary = "Perfil del usuario autenticado", description = "Devuelve id, username, email y roles del usuario del token JWT.")
    public ResponseEntity<UserResponseDTO> getCurrentUser(@AuthenticationPrincipal UserPrincipal principal) {
        UserResponseDTO dto = new UserResponseDTO();
        dto.setId(principal.getId());
        dto.setUsername(principal.getUsername());
        dto.setEmail(principal.getEmail());
        Set<String> roles = principal.getAuthorities().stream()
                .map(authority -> authority.getAuthority())
                .collect(Collectors.toSet());
        dto.setRoles(roles);
        return ResponseEntity.ok(dto);
    }
}