package ar.edu.unju.fi.securityservice;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Verifica que el contexto completo levanta con el perfil {@code test}.
 * <p>
 * El perfil activa {@code src/test/resources/application-test.yml}, que apunta a H2 en memoria
 * y desactiva Spring Cloud Config. Sin él, la aplicación buscaría el MySQL de desarrollo y el
 * test fallaría en cualquier máquina o runner de CI que no lo tenga levantado.
 * </p>
 */
@SpringBootTest
@ActiveProfiles("test")
class SecurityServiceApplicationTests {

    @Test
    void contextLoads() {
    }

}
