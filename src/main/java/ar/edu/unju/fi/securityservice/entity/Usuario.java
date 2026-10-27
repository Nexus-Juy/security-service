package ar.edu.unju.fi.securityservice.entity;

import ar.edu.unju.fi.securityservice.enums.Rol;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "usuarios")
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true, length = 80)
    private String username;

    @Column(nullable = false, unique = true, length = 120)
    private String email;

    @Column(nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Rol rol;

    @Column(name = "repartidor_id")
    private UUID repartidorId;

    public Usuario() {
    }

    public Usuario(UUID id, String username, String email, String password, Rol rol, UUID repartidorId) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.password = password;
        this.rol = rol;
        this.repartidorId = repartidorId;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Rol getRol() {
        return rol;
    }

    public void setRol(Rol rol) {
        this.rol = rol;
    }

    public UUID getRepartidorId() {
        return repartidorId;
    }

    public void setRepartidorId(UUID repartidorId) {
        this.repartidorId = repartidorId;
    }

    public static UsuarioBuilder builder() {
        return new UsuarioBuilder();
    }

    public static class UsuarioBuilder {
        private UUID id;
        private String username;
        private String email;
        private String password;
        private Rol rol;
        private UUID repartidorId;

        public UsuarioBuilder id(UUID id) {
            this.id = id;
            return this;
        }

        public UsuarioBuilder username(String username) {
            this.username = username;
            return this;
        }

        public UsuarioBuilder email(String email) {
            this.email = email;
            return this;
        }

        public UsuarioBuilder password(String password) {
            this.password = password;
            return this;
        }

        public UsuarioBuilder rol(Rol rol) {
            this.rol = rol;
            return this;
        }

        public UsuarioBuilder repartidorId(UUID repartidorId) {
            this.repartidorId = repartidorId;
            return this;
        }

        public Usuario build() {
            return new Usuario(id, username, email, password, rol, repartidorId);
        }
    }
}
