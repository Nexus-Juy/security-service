package ar.edu.unju.fi.securityservice.config;

import ar.edu.unju.fi.securityservice.entity.Usuario;
import ar.edu.unju.fi.securityservice.enums.Rol;
import ar.edu.unju.fi.securityservice.repository.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Carga los usuarios de demo al arrancar (S4-14): uno por rol, para poder probar la aplicación apenas se
 * levanta.
 * <p>
 * Como el registro público solo crea clientes, esta es la forma de tener un ADMIN y un REPARTIDOR. Se activa
 * solo si está configurada la contraseña {@code logitrack.usuarios-demo.password}, que en Docker llega por
 * la variable de entorno {@code USUARIOS_DEMO_PASSWORD}. Si un usuario ya existe, no se toca.
 * </p>
 */
@Component
public class UsuariosDemo implements ApplicationRunner {

    /** Usuario de demo con rol ADMIN. */
    public static final String ADMIN = "gw.admin";

    /** Usuario de demo con rol CLIENTE. */
    public static final String CLIENTE = "gw.cliente";

    /** Usuario de demo con rol REPARTIDOR. */
    public static final String REPARTIDOR = "gw.repartidor";

    /** Repartidor de los datos iniciales de courier-service (Brian Abalos) al que queda vinculado el REPARTIDOR. */
    public static final UUID REPARTIDOR_ID = UUID.fromString("3f6c1a52-8e0b-4d7a-9c11-0a1b2c3d4e01");

    private static final Logger log = LoggerFactory.getLogger(UsuariosDemo.class);

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final String password;

    /**
     * Crea el cargador de usuarios de demo.
     *
     * @param usuarioRepository acceso a los usuarios
     * @param passwordEncoder   cifra la contraseña con BCrypt
     * @param password          contraseña de los tres usuarios; si viene vacía no se carga nada
     */
    public UsuariosDemo(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder,
                        @Value("${logitrack.usuarios-demo.password:}") String password) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.password = password;
    }

    /**
     * Crea los tres usuarios de demo que todavía no existan.
     *
     * @param args argumentos de arranque (no se usan)
     */
    @Override
    public void run(ApplicationArguments args) {
        if (password == null || password.isBlank()) {
            log.info("Usuarios de demo desactivados: no hay contraseña configurada.");
            return;
        }
        crearSiNoExiste(ADMIN, Rol.ADMIN, null);
        crearSiNoExiste(CLIENTE, Rol.CLIENTE, null);
        crearSiNoExiste(REPARTIDOR, Rol.REPARTIDOR, REPARTIDOR_ID);
    }

    private void crearSiNoExiste(String username, Rol rol, UUID repartidorId) {
        String email = username + "@logitrack.test";
        if (usuarioRepository.existsByUsername(username) || usuarioRepository.existsByEmail(email)) {
            return;
        }
        usuarioRepository.save(Usuario.builder()
                .username(username)
                .email(email)
                .password(passwordEncoder.encode(password))
                .rol(rol)
                .repartidorId(repartidorId)
                .build());
        log.info("Usuario de demo creado: {} ({})", username, rol);
    }
}
