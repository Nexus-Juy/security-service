package ar.edu.unju.fi.securityservice.entity;

import ar.edu.unju.fi.securityservice.enums.Rol;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
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

    /** UUID del repartidor en courier-service (obligatorio si rol == REPARTIDOR). */
    @Column(name = "repartidor_id")
    private UUID repartidorId;
}
