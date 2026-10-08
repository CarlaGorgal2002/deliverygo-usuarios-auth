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

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getApellido() {
        return apellido;
    }

    public void setApellido(String apellido) {
        this.apellido = apellido;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword_hash() {
        return password_hash;
    }

    public void setPassword_hash(String password_hash) {
        this.password_hash = password_hash;
    }

    public String getTelefono() {
        return telefono;
    }

    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }

    public EstadoUsuario getEstado() {
        return estado;
    }

    public void setEstado(EstadoUsuario estado) {
        this.estado = estado;
    }

    public LocalDateTime getFecha_alta() {
        return fecha_alta;
    }

    public void setFecha_alta(LocalDateTime fecha_alta) {
        this.fecha_alta = fecha_alta;
    }

    public List<Direccion> getDirecciones() {
        return direcciones;
    }

    public void setDirecciones(List<Direccion> direcciones) {
        this.direcciones = direcciones;
    }

    public Rol getRol() {
        return rol;
    }

    public void setRol(Rol rol) {
        this.rol = rol;
    }
}
