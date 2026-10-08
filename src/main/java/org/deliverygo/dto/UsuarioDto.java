package org.deliverygo.dto;

import org.deliverygo.enums.EstadoUsuario;

import java.time.LocalDateTime;

public class UsuarioDto {

    private Long id;
    private String nombre;
    private String apellido;
    private String email;
    private String telefono;
    private EstadoUsuario estado;
    private LocalDateTime fechaAlta;
    private String rol;

    public UsuarioDto() {
    }

    public UsuarioDto(Long id, String nombre, String apellido, String email,
                      String telefono, EstadoUsuario estado, LocalDateTime fechaAlta, String rol) {
        this.id = id;
        this.nombre = nombre;
        this.apellido = apellido;
        this.email = email;
        this.telefono = telefono;
        this.estado = estado;
        this.fechaAlta = fechaAlta;
        this.rol = rol;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getApellido() { return apellido; }
    public void setApellido(String apellido) { this.apellido = apellido; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getTelefono() { return telefono; }
    public void setTelefono(String telefono) { this.telefono = telefono; }

    public EstadoUsuario getEstado() { return estado; }
    public void setEstado(EstadoUsuario estado) { this.estado = estado; }

    public LocalDateTime getFechaAlta() { return fechaAlta; }
    public void setFechaAlta(LocalDateTime fechaAlta) { this.fechaAlta = fechaAlta; }

    public String getRol() { return rol; }
    public void setRol(String rol) { this.rol = rol; }

}
