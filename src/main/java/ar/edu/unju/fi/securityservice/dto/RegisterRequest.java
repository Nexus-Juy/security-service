package ar.edu.unju.fi.securityservice.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Datos del registro público de usuarios.
 * <p>
 * El registro público siempre crea un CLIENTE (S4-14). Si el cuerpo trae otros campos, como {@code rol} o
 * {@code repartidorId}, se ignoran: nadie puede registrarse solo como ADMIN o REPARTIDOR.
 * </p>
 *
 * @param username nombre de usuario
 * @param email    email del usuario
 * @param password contraseña en claro; el máximo es 72 porque BCrypt solo usa los primeros 72 bytes y dos
 *                 contraseñas distintas podrían quedar iguales al cortarse
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record RegisterRequest(
    @NotBlank(message = "El nombre de usuario es obligatorio")
    @Size(min = 3, max = 80)
    String username,

    @NotBlank(message = "El email es obligatorio")
    @Email
    String email,

    @NotBlank(message = "La contraseña es obligatoria")
    @Size(min = 8, max = 72, message = "La contraseña debe tener entre 8 y 72 caracteres")
    String password
) {}
