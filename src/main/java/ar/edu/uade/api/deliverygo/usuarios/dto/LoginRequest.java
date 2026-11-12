package ar.edu.uade.api.deliverygo.usuarios.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import static ar.edu.uade.api.deliverygo.usuarios.dto.Validaciones.*;

// DTO-03. Se autentica con email y contraseña; no hay username.
public record LoginRequest(
        @Schema(example = "ana.perez@example.com")
        @NotBlank(message = OBLIGATORIO) @Email(regexp = EMAIL_REGEX, message = EMAIL_MENSAJE)
        String email,

        // La política de mínimo 8 es del alta; en el login sólo se exige que no esté vacía (DC-05).
        @Schema(example = "ClaveDemo2026!", accessMode = Schema.AccessMode.WRITE_ONLY)
        @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
        @NotBlank(message = OBLIGATORIO)
        String password
) {
}
