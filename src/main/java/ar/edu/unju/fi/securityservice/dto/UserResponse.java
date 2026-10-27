package ar.edu.unju.fi.securityservice.dto;


import ar.edu.unju.fi.securityservice.enums.Rol;

import java.util.UUID;

public record UserResponse(
    UUID id,
    String username,
    String email,
    Rol rol,
    UUID repartidorId
) {}
