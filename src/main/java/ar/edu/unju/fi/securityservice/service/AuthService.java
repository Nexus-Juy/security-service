package ar.edu.unju.fi.securityservice.service;


import ar.edu.unju.fi.securityservice.dto.*;

import java.util.UUID;

public interface AuthService {
    AuthResponse register(RegisterRequest request);
    AuthResponse login(LoginRequest request);
    UserResponse getCurrentUser(UUID userId);
}
