package com.eduquest.api.controller;

import com.eduquest.api.dto.response.UserResponseDTO;
import com.eduquest.api.security.UserPrincipal;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class UserControllerTest {

    private final UserController userController = new UserController();

    @Test
    void getCurrentUser_buildsResponseFromPrincipal() {
        UserPrincipal principal = new UserPrincipal(1L, "alice", "alice@utec.edu.pe", "hash",
                List.of(new SimpleGrantedAuthority("ROLE_USER")));

        ResponseEntity<UserResponseDTO> result = userController.getCurrentUser(principal);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody().getId()).isEqualTo(1L);
        assertThat(result.getBody().getUsername()).isEqualTo("alice");
        assertThat(result.getBody().getEmail()).isEqualTo("alice@utec.edu.pe");
        assertThat(result.getBody().getRoles()).isEqualTo(Set.of("ROLE_USER"));
    }
}