package com.eduquest.api.service;

import com.eduquest.api.dto.request.LoginRequestDTO;
import com.eduquest.api.dto.request.RegisterRequestDTO;
import com.eduquest.api.dto.response.AuthResponseDTO;

public interface AuthService {
    AuthResponseDTO authenticateUser(LoginRequestDTO loginRequest);
    AuthResponseDTO registerUser(RegisterRequestDTO registerRequest);
}
