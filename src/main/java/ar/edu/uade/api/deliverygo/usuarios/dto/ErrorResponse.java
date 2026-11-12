package ar.edu.uade.api.deliverygo.usuarios.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;
import java.util.List;

// DTO-08. Formato de error común del ecosistema (timestamp, status, code, message, path, correlationId).
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(
        @Schema(example = "2026-10-30T15:00:00Z") OffsetDateTime timestamp,
        @Schema(example = "400", description = "Coincide con el código HTTP.") int status,
        @Schema(example = "VALIDATION_ERROR") String code,
        @Schema(example = "La solicitud contiene datos inválidos") String message,
        @Schema(example = "/api/v1/auth/register", description = "Ruta solicitada, sin query string.") String path,
        @Schema(example = "g02-demo-001", description = "Coincide con la cabecera X-Correlation-Id.") String correlationId,
        @Schema(description = "Sólo en VALIDATION_ERROR.") List<DetalleValidacion> details
) {
}
