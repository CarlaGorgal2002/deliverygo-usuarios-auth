package ar.edu.uade.api.deliverygo.usuarios.dto;

import ar.edu.uade.api.deliverygo.usuarios.domain.EstadoUsuario;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;

// DTO-02. Sin contraseña ni hash; las direcciones se consultan por separado (API-05).
public record UsuarioResponse(
        @Schema(example = "1") Long id,
        @Schema(example = "Ana") String nombre,
        @Schema(example = "Pérez") String apellido,
        @Schema(example = "ana.perez@example.com") String email,
        @Schema(example = "+54 11 5555 0101", nullable = true) String telefono,
        @Schema(example = "CLIENTE", allowableValues = {"CLIENTE", "COMERCIO", "REPARTIDOR", "ADMIN"}) String rol,
        EstadoUsuario estado,
        @Schema(example = "2026-10-30T15:00:00Z", description = "ISO-8601 en UTC (DC-04).") OffsetDateTime fechaAlta
) {
}
