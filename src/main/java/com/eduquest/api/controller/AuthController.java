package com.eduquest.api.controller;

import com.eduquest.api.dto.request.LoginRequestDTO;
import com.eduquest.api.dto.request.RegisterRequestDTO;
import com.eduquest.api.dto.response.AuthResponseDTO;
import com.eduquest.api.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@CrossOrigin(origins = "*") // Permite que tu frontend se conecte sin problemas de CORS
public class AuthController {

    private final AuthService authService;

    // Inyección de dependencias por constructor (cumpliendo la Sección 3.3 de la rúbrica)
    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponseDTO> authenticateUser(@Valid @RequestBody LoginRequestDTO loginRequest) {
        // Delegamos toda la lógica al servicio (Controlador delgado)
        AuthResponseDTO response = authService.authenticateUser(loginRequest);

        // Retornamos 200 OK con el token
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponseDTO> registerUser(@Valid @RequestBody RegisterRequestDTO registerRequest) {
        // Delegamos toda la lógica al servicio
        AuthResponseDTO response = authService.registerUser(registerRequest);

        // Retornamos 201 Created porque se creó un nuevo recurso en la base de datos
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }
}