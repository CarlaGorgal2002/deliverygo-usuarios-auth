package ar.edu.uade.api.deliverygo.usuarios.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import static ar.edu.uade.api.deliverygo.usuarios.dto.Validaciones.*;

// DTO-13 (CU-08). Token recibido por Notificaciones y la contraseña nueva, que sigue la política del alta.
public record RestablecerPasswordRequest(
        @Schema(example = "Qm9X2n8vKq1d0sT7yWJ4cFhZpL6aRu3eBgN5iVxMoDk", description = "Token de un solo uso entregado por Notificaciones.")
        @NotBlank(message = OBLIGATORIO) @Size(max = 128, message = "No puede superar 128 caracteres")
        String token,

        @Schema(example = "NuevaClave2026!", accessMode = Schema.AccessMode.WRITE_ONLY, description = "Mínimo 8 caracteres. Nunca se devuelve ni se registra.")
        @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
        @NotBlank(message = OBLIGATORIO) @Size(min = 8, message = PASSWORD_MINIMO)
        String password
) {
}
