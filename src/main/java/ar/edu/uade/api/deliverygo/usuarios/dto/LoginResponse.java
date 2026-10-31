package ar.edu.uade.api.deliverygo.usuarios.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;

// DTO-04. Token y datos mínimos para el Portal; la autorización real la valida cada backend.
public record LoginResponse(
        @Schema(description = "JWT firmado. Enviarlo como Authorization: Bearer <token>.") String token,
        @Schema(example = "Bearer") String tipo,
        @Schema(example = "2026-10-30T16:00:00Z", description = "Vencimiento absoluto; coincide con el claim exp.") OffsetDateTime expiraEn,
        @Schema(example = "1") Long usuarioId,
        @Schema(example = "Ana") String nombre,
        @Schema(example = "CLIENTE", allowableValues = {"CLIENTE", "COMERCIO", "REPARTIDOR", "ADMIN"}) String rol
) {
}
