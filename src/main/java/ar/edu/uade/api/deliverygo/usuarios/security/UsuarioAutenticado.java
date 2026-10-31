package ar.edu.uade.api.deliverygo.usuarios.security;

// Identidad que el filtro JWT deja en el SecurityContext: sub (id) y role del token.
public record UsuarioAutenticado(Long id, String rol) {
}
