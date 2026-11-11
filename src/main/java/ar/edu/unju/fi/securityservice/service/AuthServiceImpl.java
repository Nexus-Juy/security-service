package ar.edu.unju.fi.securityservice.service;

import ar.edu.unju.fi.securityservice.dto.*;
import ar.edu.unju.fi.securityservice.enums.Rol;
import ar.edu.unju.fi.securityservice.entity.Usuario;
import ar.edu.unju.fi.securityservice.exception.BadCredentialsException;
import ar.edu.unju.fi.securityservice.exception.ResourceNotFoundException;
import ar.edu.unju.fi.securityservice.exception.UserAlreadyExistsException;
import ar.edu.unju.fi.securityservice.repository.UsuarioRepository;
import ar.edu.unju.fi.securityservice.service.jwt.JwtUtils;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
public class AuthServiceImpl implements AuthService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;

    /**
     * Hash de una contraseña que nadie conoce. Cuando el usuario no existe se compara contra él para que el
     * login tarde lo mismo que con un usuario real y no revele, por el tiempo de respuesta, qué cuentas existen.
     */
    private final String hashFicticio;

    public AuthServiceImpl(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder, JwtUtils jwtUtils) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtils = jwtUtils;
        this.hashFicticio = passwordEncoder.encode(UUID.randomUUID().toString());
    }

    /**
     * Registra un usuario desde el registro público.
     * <p>
     * Siempre crea un CLIENTE sin repartidor vinculado (S4-14). Los ADMIN y los REPARTIDOR no se crean por
     * acá: para probar están los usuarios de demo que carga {@code UsuariosDemo} al arrancar.
     * </p>
     *
     * @param request datos del registro
     * @return el token y los datos del usuario creado
     * @throws UserAlreadyExistsException si el nombre de usuario o el email ya están registrados
     */
    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (usuarioRepository.existsByUsername(request.username())) {
            throw new UserAlreadyExistsException("El nombre de usuario '%s' ya está registrado.".formatted(request.username()));
        }
        if (usuarioRepository.existsByEmail(request.email())) {
            throw new UserAlreadyExistsException("El email '%s' ya está registrado.".formatted(request.email()));
        }

        Usuario nuevoUsuario = Usuario.builder()
            .username(request.username())
            .email(request.email())
            .password(passwordEncoder.encode(request.password()))
            .rol(Rol.CLIENTE)
            .build();

        Usuario guardado = usuarioRepository.save(nuevoUsuario);
        String token = jwtUtils.generateToken(guardado);

        return new AuthResponse(token, guardado.getId(), guardado.getUsername(), guardado.getRol().name());
    }

    @Override
    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        Optional<Usuario> encontrado = usuarioRepository.findByUsername(request.usernameOrEmail())
            .or(() -> usuarioRepository.findByEmail(request.usernameOrEmail()));

        if (encontrado.isEmpty()) {
            passwordEncoder.matches(request.password(), hashFicticio);
            throw new BadCredentialsException("Credenciales inválidas.");
        }

        Usuario usuario = encontrado.get();
        if (!passwordEncoder.matches(request.password(), usuario.getPassword())) {
            throw new BadCredentialsException("Credenciales inválidas.");
        }

        String token = jwtUtils.generateToken(usuario);
        return new AuthResponse(token, usuario.getId(), usuario.getUsername(), usuario.getRol().name());
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getCurrentUser(UUID userId) {
        Usuario usuario = usuarioRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con ID: " + userId));

        return new UserResponse(
            usuario.getId(),
            usuario.getUsername(),
            usuario.getEmail(),
            usuario.getRol(),
            usuario.getRepartidorId()
        );
    }
}
