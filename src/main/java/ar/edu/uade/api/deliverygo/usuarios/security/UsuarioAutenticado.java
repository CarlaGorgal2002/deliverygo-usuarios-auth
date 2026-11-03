package ar.edu.uade.api.deliverygo.usuarios.security;

import java.time.Instant;

// Identidad que el filtro JWT deja en el SecurityContext: sub (id), role y exp del token.
public record UsuarioAutenticado(Long id, String rol, Instant expiraEn) {
}
