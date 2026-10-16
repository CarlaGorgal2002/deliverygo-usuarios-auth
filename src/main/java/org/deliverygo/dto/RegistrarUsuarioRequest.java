package org.deliverygo.dto;

public class RegistrarUsuarioRequest {
    private String nombre;
    private String apellido;
    private String email;
    private String password;
    private String telefono;
    private Long rolId;

    public RegistrarUsuarioRequest() {
    }

    public RegistrarUsuarioRequest(String nombre, String apellido, String email, String password, String telefono, Long rolId) {
        this.nombre = nombre;
        this.apellido = apellido;
        this.email = email;
        this.password = password;
        this.telefono = telefono;
        this.rolId = rolId;
    }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getApellido() { return apellido; }
    public void setApellido(String apellido) { this.apellido = apellido; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getTelefono() { return telefono; }
    public void setTelefono(String telefono) { this.telefono = telefono; }

    public Long getRolId() { return rolId; }
    public void setRolId(Long rolId) { this.rolId = rolId; }
}