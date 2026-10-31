package ar.edu.uade.api.deliverygo.usuarios.dto;

import io.swagger.v3.oas.annotations.media.Schema;

// DTO-07. Sólo direcciones activas; no expone activa ni la entidad Usuario.
public record DireccionResponse(
        @Schema(example = "10") Long id,
        @Schema(example = "Av. Independencia") String calle,
        @Schema(example = "1200") String numero,
        @Schema(example = "2", nullable = true) String piso,
        @Schema(example = "B", nullable = true) String departamento,
        @Schema(example = "Ciudad Autónoma de Buenos Aires") String ciudad,
        @Schema(example = "Ciudad Autónoma de Buenos Aires") String provincia,
        @Schema(example = "C1099AAZ") String codigoPostal,
        boolean predeterminada
) {
}
