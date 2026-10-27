package ar.edu.unju.fi.securityservice.service.jwt;


import ar.edu.unju.fi.securityservice.entity.Usuario;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
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

    @Value("${jwt.secret:ClaveSecretaSuperSeguraParaFirmarTokensJWTLogiTrack2026!}")
    private String jwtSecret;

    @Value("${jwt.expiration-ms:86400000}")
    private long jwtExpirationMs;

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
