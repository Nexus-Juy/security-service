package ar.edu.unju.fi.securityservice.controller;

import ar.edu.unju.fi.securityservice.util.JwtUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Expone la autenticación en http://localhost:8080/api/v1/auth/login
 */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final JwtUtils jwtUtils;

    public AuthController(JwtUtils jwtUtils) {
        this.jwtUtils = jwtUtils;
    }

    public record LoginRequest(String username, String password) {}

    @PostMapping("/login")
    public ResponseEntity<Map<String, String>> login(@RequestBody LoginRequest request) {
        // Validación de credenciales de prueba
        if ("logitrack".equals(request.username()) && "logitrack".equals(request.password())) {
            String token = jwtUtils.generateToken(request.username(), "ROLE_ADMIN");
            return ResponseEntity.ok(Map.of(
                "token", token,
                "type", "Bearer",
                "expiresIn", "86400"
            ));
        }
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }
}
