package ar.edu.unju.fi.securityservice.service.jwt;


import ar.edu.unju.fi.securityservice.entity.Usuario;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class JwtUtils {

    /** HS256 exige una clave de al menos 256 bits (32 bytes). */
    private static final int LONGITUD_MINIMA_SECRETO_BYTES = 32;

    /**
     * Secreto de firma. No tiene valor por defecto a propósito: un secreto escrito en el código fuente es
     * público y cualquiera podría falsificar tokens. Si falta {@code JWT_SECRET}, la aplicación no arranca.
     */
    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.expiration-ms:86400000}")
    private long jwtExpirationMs;

    /** Falla al iniciar (en lugar de en el primer login) si el secreto es demasiado corto. */
    @PostConstruct
    void validarSecreto() {
        if (jwtSecret == null || jwtSecret.getBytes(StandardCharsets.UTF_8).length < LONGITUD_MINIMA_SECRETO_BYTES) {
            throw new IllegalStateException(
                    "jwt.secret debe tener al menos %d caracteres (HS256 exige una clave de 256 bits)."
                            .formatted(LONGITUD_MINIMA_SECRETO_BYTES));
        }
    }

    private Key getSigningKey() {
        byte[] keyBytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * Genera un JWT firmado que incluye los claims exigidos para Sprint 2:
     * - sub: ID único del usuario (UUID)
     * - roles: Lista de roles sin prefijo (ej: ["ADMIN"], ["CLIENTE"])
     * - repartidorId: UUID del repartidor (solo si el rol es REPARTIDOR)
     */
    public String generateToken(Usuario usuario) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("roles", List.of(usuario.getRol().name()));

        if (usuario.getRepartidorId() != null) {
            claims.put("repartidorId", usuario.getRepartidorId().toString());
        }

        return Jwts.builder()
            .setClaims(claims)
            .setSubject(usuario.getId().toString())
            .setIssuedAt(new Date())
            .setExpiration(new Date(System.currentTimeMillis() + jwtExpirationMs))
            .signWith(getSigningKey(), SignatureAlgorithm.HS256)
            .compact();
    }

    public String getUserIdFromToken(String token) {
        return getClaims(token).getSubject();
    }

    public boolean validateToken(String token) {
        try {
            getClaims(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private Claims getClaims(String token) {
        return Jwts.parserBuilder()
            .setSigningKey(getSigningKey())
            .build()
            .parseClaimsJws(token)
            .getBody();
    }
}
