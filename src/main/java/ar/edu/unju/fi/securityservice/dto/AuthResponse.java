package ar.edu.unju.fi.securityservice.dto;


import java.util.UUID;

public record AuthResponse(
    String token,
    String type,
    UUID userId,
    String username,
    String role
) {
    public AuthResponse(String token, UUID userId, String username, String role) {
        this(token, "Bearer", userId, username, role);
    }
}
