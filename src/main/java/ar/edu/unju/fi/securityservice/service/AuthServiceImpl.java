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

import java.util.UUID;

@Service
public class AuthServiceImpl implements AuthService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;

    public AuthServiceImpl(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder, JwtUtils jwtUtils) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtils = jwtUtils;
    }

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (usuarioRepository.existsByUsername(request.username())) {
            throw new UserAlreadyExistsException("El nombre de usuario '%s' ya está registrado.".formatted(request.username()));
        }
        if (usuarioRepository.existsByEmail(request.email())) {
            throw new UserAlreadyExistsException("El email '%s' ya está registrado.".formatted(request.email()));
        }

        // Si no se especifica rol, asigna CLIENTE por defecto
        Rol rolAsignado = request.rol() != null ? request.rol() : Rol.CLIENTE;

        if (rolAsignado == Rol.REPARTIDOR && request.repartidorId() == null) {
            throw new IllegalArgumentException("El repartidorId es obligatorio para usuarios con rol REPARTIDOR.");
        }

        Usuario nuevoUsuario = Usuario.builder()
            .username(request.username())
            .email(request.email())
            .password(passwordEncoder.encode(request.password()))
            .rol(rolAsignado)
            .repartidorId(rolAsignado == Rol.REPARTIDOR ? request.repartidorId() : null)
            .build();

        Usuario guardado = usuarioRepository.save(nuevoUsuario);
        String token = jwtUtils.generateToken(guardado);

        return new AuthResponse(token, guardado.getId(), guardado.getUsername(), guardado.getRol().name());
    }

    @Override
    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        Usuario usuario = usuarioRepository.findByUsername(request.usernameOrEmail())
            .orElseGet(() -> usuarioRepository.findByEmail(request.usernameOrEmail())
                .orElseThrow(() -> new BadCredentialsException("Credenciales inválidas.")));

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
