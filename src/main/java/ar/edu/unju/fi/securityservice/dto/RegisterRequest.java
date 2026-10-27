package ar.edu.unju.fi.securityservice.dto;


import ar.edu.unju.fi.securityservice.enums.Rol;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record RegisterRequest(
    @NotBlank(message = "El nombre de usuario es obligatorio")
    @Size(min = 3, max = 80)
    String username,

    @NotBlank(message = "El email es obligatorio")
    @Email
    String email,

    @NotBlank(message = "La contraseña es obligatoria")
    @Size(min = 6)
    String password,

    Rol rol,

    UUID repartidorId
) {}
