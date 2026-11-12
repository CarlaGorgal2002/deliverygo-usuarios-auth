package ar.edu.uade.api.deliverygo.usuarios.security;

// Identidad que el filtro de API Key deja en el SecurityContext cuando un módulo consumidor
// se presenta con su credencial propia (RN-15). No es un usuario ni tiene rol de usuario.
public record ModuloAutenticado(String nombre) {
}
