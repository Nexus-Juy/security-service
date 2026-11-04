package ar.edu.unju.fi.securityservice.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifica el cifrado de contraseñas: BCrypt con el costo configurado, sal aleatoria por hash y
 * verificación correcta, sin levantar Spring.
 */
class SecurityConfigTest {

    private final PasswordEncoder encoder =
            new SecurityConfig().passwordEncoder(SecurityConfig.COSTO_BCRYPT_POR_DEFECTO);

    @Test
    @DisplayName("El hash es BCrypt con costo 12 y no contiene la contraseña en claro")
    void generaHashBcryptConCostoEsperado() {
        String hash = encoder.encode("secreto123");

        assertThat(hash).startsWith("$2a$12$");
        assertThat(hash).doesNotContain("secreto123");
    }

    @Test
    @DisplayName("La misma contraseña genera hashes distintos (sal aleatoria) y ambos validan")
    void cadaHashLlevaSuPropiaSal() {
        String primero = encoder.encode("secreto123");
        String segundo = encoder.encode("secreto123");

        assertThat(primero).isNotEqualTo(segundo);
        assertThat(encoder.matches("secreto123", primero)).isTrue();
        assertThat(encoder.matches("secreto123", segundo)).isTrue();
    }

    @Test
    @DisplayName("Una contraseña incorrecta no valida")
    void rechazaContrasenaIncorrecta() {
        String hash = encoder.encode("secreto123");

        assertThat(encoder.matches("otra-clave", hash)).isFalse();
    }
}
