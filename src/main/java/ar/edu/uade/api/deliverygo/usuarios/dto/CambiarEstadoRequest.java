package ar.edu.uade.api.deliverygo.usuarios.dto;

import ar.edu.uade.api.deliverygo.usuarios.domain.EstadoUsuario;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import static ar.edu.uade.api.deliverygo.usuarios.dto.Validaciones.OBLIGATORIO;

// DTO-10 (CU-06). Sólo el estado: el resto de la cuenta no se toca por esta vía.
public record CambiarEstadoRequest(
        @Schema(example = "INACTIVO", description = "Estado deseado. Repetir el mismo estado es idempotente.")
        @NotNull(message = OBLIGATORIO)
        EstadoUsuario estado
) {
}
