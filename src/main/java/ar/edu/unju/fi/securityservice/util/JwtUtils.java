package ar.edu.unju.fi.securityservice.util;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * Utilitario para la firma y generación de tokens JWT.
 */
@Component
public class JwtUtils {

    @Value("${jwt.secret:ClaveSecretaSuperSeguraParaFirmarLosTokensJWT123456}")
    private String secret;

    @Value("${jwt.expiration:86400000}") // 24 horas
    private long expiration;

    public String generateToken(String username, String role) {
        return Jwts.builder()
            .setSubject(username)
            .claim("role", role)
            .setIssuedAt(new Date())
            .setExpiration(new Date(System.currentTimeMillis() + expiration))
            .signWith(Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8)), SignatureAlgorithm.HS256)
            .compact();
    }
}
