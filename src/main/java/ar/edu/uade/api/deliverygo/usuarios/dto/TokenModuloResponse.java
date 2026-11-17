package ar.edu.uade.api.deliverygo.usuarios.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;

// DTO-17. JWT con rol MODULO para llamar a los recursos de lectura de Usuarios.
public record TokenModuloResponse(
        @Schema(description = "JWT firmado. Enviarlo como Authorization: Bearer <token>.") String token,
        @Schema(example = "Bearer") String tipo,
        @Schema(example = "2026-10-30T16:00:00Z", description = "Vencimiento absoluto; coincide con el claim exp.") OffsetDateTime expiraEn,
        @Schema(example = "pedidos") String modulo,
        @Schema(example = "MODULO") String rol
) {
}
