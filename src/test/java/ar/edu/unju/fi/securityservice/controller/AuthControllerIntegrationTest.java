package ar.edu.unju.fi.securityservice.controller;

import ar.edu.unju.fi.securityservice.entity.Usuario;
import ar.edu.unju.fi.securityservice.enums.Rol;
import ar.edu.unju.fi.securityservice.repository.UsuarioRepository;
import com.jayway.jsonpath.JsonPath;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * IntegraciÃ³n de la API de autenticaciÃ³n sobre H2: registro, login y consulta del usuario
 * autenticado, incluyendo los casos de credenciales invÃ¡lidas y tokens invÃ¡lidos/expirados.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerIntegrationTest {

    private static final String AUTH = "/api/v1/auth";
    private static final String SECRETO_TEST =
            "ClaveSecretaDeTestSuperSeguraParaTestsUnitariosLogiTrack2026!";

    private static final String BODY_REGISTRO = """
            {
              "username": "lucia",
              "email": "lucia@logitrack.com",
              "password": "secreto123"
            }
            """;

    private static final String BODY_LOGIN = """
            {
              "usernameOrEmail": "lucia",
              "password": "secreto123"
            }
            """;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @BeforeEach
    void limpiarUsuarios() {
        usuarioRepository.deleteAll();
    }

    private String registrarYObtenerToken() throws Exception {
        String respuesta = mockMvc.perform(post(AUTH + "/register")
                        .contentType(MediaType.APPLICATION_JSON).content(BODY_REGISTRO))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(respuesta, "$.token");
    }

    private String tokenExpirado() {
        return Jwts.builder()
                .setSubject(UUID.randomUUID().toString())
                .setIssuedAt(new Date(System.currentTimeMillis() - 7_200_000L))
                .setExpiration(new Date(System.currentTimeMillis() - 3_600_000L))
                .signWith(Keys.hmacShaKeyFor(SECRETO_TEST.getBytes(StandardCharsets.UTF_8)),
                        SignatureAlgorithm.HS256)
                .compact();
    }

    @Test
    @DisplayName("Registro: devuelve 201 con un JWT y el rol CLIENTE por defecto")
    void registroDevuelve201ConToken() throws Exception {
        mockMvc.perform(post(AUTH + "/register")
                        .contentType(MediaType.APPLICATION_JSON).content(BODY_REGISTRO))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.type").value("Bearer"))
                .andExpect(jsonPath("$.username").value("lucia"))
                .andExpect(jsonPath("$.role").value("CLIENTE"))
                .andExpect(jsonPath("$.userId").isNotEmpty());

        assertThat(usuarioRepository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("Registro: 409 si el email ya existe")
    void registroDuplicadoDevuelve409() throws Exception {
        mockMvc.perform(post(AUTH + "/register")
                .contentType(MediaType.APPLICATION_JSON).content(BODY_REGISTRO));

        mockMvc.perform(post(AUTH + "/register")
                        .contentType(MediaType.APPLICATION_JSON).content(BODY_REGISTRO))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    /**
     * S4-14: aunque el cuerpo pida rol ADMIN, el registro público crea un CLIENTE.
     *
     * @throws Exception si falla la petición
     */
    @Test
    @DisplayName("Registro: aunque pida rol ADMIN, se crea como CLIENTE")
    void registroConRolAdminCreaCliente() throws Exception {
        String body = """
                {
                  "username": "intruso",
                  "email": "intruso@logitrack.com",
                  "password": "secreto123",
                  "rol": "ADMIN"
                }
                """;

        mockMvc.perform(post(AUTH + "/register")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("CLIENTE"));

        assertThat(usuarioRepository.findByUsername("intruso").orElseThrow().getRol()).isEqualTo(Rol.CLIENTE);
    }

    /**
     * S4-14: tampoco se puede registrar un REPARTIDOR; se crea un CLIENTE sin repartidor vinculado.
     *
     * @throws Exception si falla la petición
     */
    @Test
    @DisplayName("Registro: aunque pida REPARTIDOR con repartidorId, se crea CLIENTE sin repartidor")
    void registroConRolRepartidorCreaClienteSinRepartidor() throws Exception {
        String body = """
                {
                  "username": "carlos",
                  "email": "carlos@logitrack.com",
                  "password": "secreto123",
                  "rol": "REPARTIDOR",
                  "repartidorId": "3f6c1a52-8e0b-4d7a-9c11-0a1b2c3d4e01"
                }
                """;

        mockMvc.perform(post(AUTH + "/register")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("CLIENTE"));

        Usuario carlos = usuarioRepository.findByUsername("carlos").orElseThrow();
        assertThat(carlos.getRol()).isEqualTo(Rol.CLIENTE);
        assertThat(carlos.getRepartidorId()).isNull();
    }

    @Test
    @DisplayName("Registro: 400 si la contraseÃ±a es demasiado corta")
    void registroConPasswordCortaDevuelve400() throws Exception {
        String body = """
                {
                  "username": "lucia",
                  "email": "lucia@logitrack.com",
                  "password": "123"
                }
                """;

        mockMvc.perform(post(AUTH + "/register")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @DisplayName("Login: 200 con token tanto por username como por email")
    void loginDevuelveTokenPorUsernameYPorEmail() throws Exception {
        registrarYObtenerToken();

        mockMvc.perform(post(AUTH + "/login")
                        .contentType(MediaType.APPLICATION_JSON).content(BODY_LOGIN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.role").value("CLIENTE"));

        String porEmail = """
                {
                  "usernameOrEmail": "lucia@logitrack.com",
                  "password": "secreto123"
                }
                """;
        mockMvc.perform(post(AUTH + "/login")
                        .contentType(MediaType.APPLICATION_JSON).content(porEmail))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty());
    }

    @Test
    @DisplayName("Login: 401 con contraseÃ±a incorrecta")
    void loginConPasswordIncorrectaDevuelve401() throws Exception {
        registrarYObtenerToken();

        String body = """
                {
                  "usernameOrEmail": "lucia",
                  "password": "otra-clave"
                }
                """;

        mockMvc.perform(post(AUTH + "/login")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    @DisplayName("Login: 401 si el usuario no existe")
    void loginConUsuarioInexistenteDevuelve401() throws Exception {
        mockMvc.perform(post(AUTH + "/login")
                        .contentType(MediaType.APPLICATION_JSON).content(BODY_LOGIN))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("/me: devuelve el usuario autenticado cuando el token es vÃ¡lido")
    void meDevuelveUsuarioConTokenValido() throws Exception {
        String token = registrarYObtenerToken();

        mockMvc.perform(get(AUTH + "/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("lucia"))
                .andExpect(jsonPath("$.email").value("lucia@logitrack.com"))
                .andExpect(jsonPath("$.rol").value("CLIENTE"));
    }

    @Test
    @DisplayName("/me: 401 sin header Authorization")
    void meSinTokenDevuelve401() throws Exception {
        mockMvc.perform(get(AUTH + "/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("/me: 401 con token invÃ¡lido")
    void meConTokenInvalidoDevuelve401() throws Exception {
        mockMvc.perform(get(AUTH + "/me").header("Authorization", "Bearer token-falso"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("/me: 401 con token expirado")
    void meConTokenExpiradoDevuelve401() throws Exception {
        mockMvc.perform(get(AUTH + "/me").header("Authorization", "Bearer " + tokenExpirado()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("/me: 401 con un esquema que no es Bearer")
    void meConEsquemaIncorrectoDevuelve401() throws Exception {
        String token = registrarYObtenerToken();

        mockMvc.perform(get(AUTH + "/me").header("Authorization", "Basic " + token))
                .andExpect(status().isUnauthorized());
    }
}
