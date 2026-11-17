package ar.edu.uade.api.deliverygo.usuarios.security;

// Identidad que el filtro JWT deja en el SecurityContext cuando el Bearer es un token de módulo
// consumidor (rol MODULO, RN-15). No es un usuario ni tiene rol de usuario.
public record ModuloAutenticado(String nombre) {
}
