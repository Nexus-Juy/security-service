package ar.edu.unju.fi.securityservice.dto;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
    @NotBlank(message = "El usuario o email es obligatorio")
    String usernameOrEmail,

    @NotBlank(message = "La contraseña es obligatoria")
    String password
) {}
