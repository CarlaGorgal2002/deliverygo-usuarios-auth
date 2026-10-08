package org.deliverygo.model;

import jakarta.persistence.*;
import org.deliverygo.enums.EstadoUsuario;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "usuarios")
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nombre;

    @Column(nullable = false)
    private String apellido;

    @Column(unique = true)
    private String email;

    @Column(nullable = false)
    private String password_hash;

    @Column(nullable = false)
    private String telefono;

    @Enumerated(EnumType.STRING)
    private EstadoUsuario estado;

    private LocalDateTime fecha_alta;

    @OneToMany(mappedBy = "usuario")
    private List<Direccion> direcciones = new ArrayList<>();

    @ManyToOne()
    @JoinColumn(name = "rol_id")
    private Rol rol;

    public Usuario() {}

    public Usuario (String nombre, String apellido, String email, String password_hash, String telefono) {
        this.nombre = nombre;
        this.apellido = apellido;
        this.email = email;
        this.password_hash = password_hash;
        this.telefono = telefono;
        this.estado = EstadoUsuario.ACTIVO;

        this.fecha_alta = LocalDateTime.now();
    }
}
