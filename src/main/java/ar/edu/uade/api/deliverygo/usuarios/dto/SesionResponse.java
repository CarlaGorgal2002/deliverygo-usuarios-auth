package ar.edu.uade.api.deliverygo.usuarios.dto;

import ar.edu.uade.api.deliverygo.usuarios.domain.EstadoUsuario;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;

// DTO-14 (CU-09). Resultado de validar un JWT contra la base: el rol y el estado son los actuales,
// no los que quedaron escritos en el token.
public record SesionResponse(
        @Schema(example = "1") Long usuarioId,
        @Schema(example = "CLIENTE", allowableValues = {"CLIENTE", "COMERCIO", "REPARTIDOR", "ADMIN"}) String rol,
        EstadoUsuario estado,
        @Schema(example = "2026-10-30T16:00:00Z", description = "Vencimiento del token presentado (claim exp).") OffsetDateTime expiraEn
) {
}
