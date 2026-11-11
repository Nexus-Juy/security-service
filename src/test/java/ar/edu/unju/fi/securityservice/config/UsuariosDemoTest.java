package ar.edu.unju.fi.securityservice.config;

import ar.edu.unju.fi.securityservice.entity.Usuario;
import ar.edu.unju.fi.securityservice.enums.Rol;
import ar.edu.unju.fi.securityservice.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * Usuarios de demo (S4-14): se carga uno por rol al arrancar, sin duplicarlos y solo si hay contraseña.
 */
@SpringBootTest(properties = "logitrack.usuarios-demo.password=" + UsuariosDemoTest.PASSWORD)
@ActiveProfiles("test")
class UsuariosDemoTest {

    static final String PASSWORD = "demo-12345";

    @Autowired
    private UsuariosDemo usuariosDemo;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    /** Cada prueba arranca con la base vacía y los usuarios de demo recién cargados. */
    @BeforeEach
    void cargarUsuariosDemo() {
        usuarioRepository.deleteAll();
        usuariosDemo.run(null);
    }

    /** Se crea un usuario por rol, con la contraseña configurada cifrada con BCrypt. */
    @Test
    @DisplayName("Carga gw.admin, gw.cliente y gw.repartidor con su rol y la contraseña configurada")
    void cargaUnUsuarioPorRol() {
        Usuario admin = usuarioRepository.findByUsername(UsuariosDemo.ADMIN).orElseThrow();
        Usuario cliente = usuarioRepository.findByUsername(UsuariosDemo.CLIENTE).orElseThrow();
        Usuario repartidor = usuarioRepository.findByUsername(UsuariosDemo.REPARTIDOR).orElseThrow();

        assertThat(admin.getRol()).isEqualTo(Rol.ADMIN);
        assertThat(cliente.getRol()).isEqualTo(Rol.CLIENTE);
        assertThat(repartidor.getRol()).isEqualTo(Rol.REPARTIDOR);
        assertThat(repartidor.getRepartidorId()).isEqualTo(UsuariosDemo.REPARTIDOR_ID);
        assertThat(admin.getRepartidorId()).isNull();
        assertThat(admin.getEmail()).isEqualTo("gw.admin@logitrack.test");
        assertThat(passwordEncoder.matches(PASSWORD, admin.getPassword())).isTrue();
        assertThat(admin.getPassword()).doesNotContain(PASSWORD);
    }

    /** Volver a arrancar no duplica los usuarios que ya existen. */
    @Test
    @DisplayName("Si los usuarios ya existen, no los vuelve a crear")
    void noDuplicaUsuarios() {
        usuariosDemo.run(null);

        assertThat(usuarioRepository.count()).isEqualTo(3);
    }

    /** Sin contraseña configurada no se toca la base. */
    @Test
    @DisplayName("Sin contraseña configurada no carga nada")
    void sinPasswordNoCargaNada() {
        UsuarioRepository repositorio = mock(UsuarioRepository.class);

        new UsuariosDemo(repositorio, passwordEncoder, "").run(null);

        verifyNoInteractions(repositorio);
    }
}
