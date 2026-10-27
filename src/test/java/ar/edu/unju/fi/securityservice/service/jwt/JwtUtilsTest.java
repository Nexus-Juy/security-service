package ar.edu.unju.fi.securityservice.service.jwt;

import ar.edu.unju.fi.securityservice.entity.Usuario;
import ar.edu.unju.fi.securityservice.enums.Rol;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests unitarios de {@link JwtUtils}: firma, claims y validación, sin levantar Spring.
 */
class JwtUtilsTest {

    private static final String SECRETO = "ClaveSecretaDeTestSuperSeguraParaTestsUnitariosLogiTrack2026!";
    private static final long UNA_HORA = 3_600_000L;

    private JwtUtils jwtUtils;
    private UUID usuarioId;

    @BeforeEach
    void setUp() {
        jwtUtils = new JwtUtils();
        ReflectionTestUtils.setField(jwtUtils, "jwtSecret", SECRETO);
        ReflectionTestUtils.setField(jwtUtils, "jwtExpirationMs", UNA_HORA);
        usuarioId = UUID.randomUUID();
    }

    private Usuario usuario(Rol rol, UUID repartidorId) {
        Usuario usuario = Usuario.builder()
                .username("lucia")
                .email("lucia@logitrack.com")
                .password("hash-bcrypt")
                .rol(rol)
                .repartidorId(repartidorId)
                .build();
        usuario.setId(usuarioId);
        return usuario;
    }

    private Claims claims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(Keys.hmacShaKeyFor(SECRETO.getBytes(StandardCharsets.UTF_8)))
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    @Test
    @DisplayName("El token lleva el id del usuario en el subject y el rol en roles")
    void generaTokenConSubjectYRol() {
        String token = jwtUtils.generateToken(usuario(Rol.CLIENTE, null));

        Claims claims = claims(token);

        assertThat(claims.getSubject()).isEqualTo(usuarioId.toString());
        @SuppressWarnings("unchecked")
        List<String> roles = (List<String>) claims.get("roles");
        assertThat(roles).containsExactly("CLIENTE");
        assertThat(claims.get("repartidorId", String.class)).isNull();
        assertThat(claims.getExpiration()).isAfter(claims.getIssuedAt());
    }

    @Test
    @DisplayName("Solo los REPARTIDOR llevan el claim repartidorId")
    void incluyeRepartidorIdSoloParaRepartidor() {
        UUID repartidorId = UUID.randomUUID();

        String token = jwtUtils.generateToken(usuario(Rol.REPARTIDOR, repartidorId));

        assertThat(claims(token).get("repartidorId", String.class)).isEqualTo(repartidorId.toString());
    }

    @Test
    @DisplayName("Un token recién firmado es válido y devuelve el id del usuario")
    void validaTokenFirmado() {
        String token = jwtUtils.generateToken(usuario(Rol.ADMIN, null));

        assertThat(jwtUtils.validateToken(token)).isTrue();
        assertThat(jwtUtils.getUserIdFromToken(token)).isEqualTo(usuarioId.toString());
    }

    @Test
    @DisplayName("Un token con la firma alterada se rechaza")
    void rechazaTokenConFirmaInvalida() {
        String token = jwtUtils.generateToken(usuario(Rol.CLIENTE, null));
        char ultimo = token.charAt(token.length() - 1);
        String alterado = token.substring(0, token.length() - 1) + (ultimo == 'A' ? 'B' : 'A');

        assertThat(jwtUtils.validateToken(alterado)).isFalse();
    }

    @Test
    @DisplayName("Un token expirado se rechaza")
    void rechazaTokenExpirado() {
        ReflectionTestUtils.setField(jwtUtils, "jwtExpirationMs", -1_000L);

        String expirado = jwtUtils.generateToken(usuario(Rol.CLIENTE, null));

        assertThat(jwtUtils.validateToken(expirado)).isFalse();
    }

    @Test
    @DisplayName("Un texto que no es un JWT se rechaza sin lanzar excepción")
    void rechazaTokenMalformado() {
        assertThat(jwtUtils.validateToken("esto-no-es-un-token")).isFalse();
        assertThat(jwtUtils.validateToken("")).isFalse();
    }
}
