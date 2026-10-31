package ar.edu.uade.api.deliverygo.usuarios.security;

import java.time.Instant;

public record TokenEmitido(String token, Instant expiraEn) {
}
