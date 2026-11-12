package ar.edu.uade.api.deliverygo.usuarios.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import static ar.edu.uade.api.deliverygo.usuarios.dto.Validaciones.*;

// DTO-12 (CU-08). Sólo el email de la cuenta.
public record SolicitarRecuperacionRequest(
        @Schema(example = "ana.perez@example.com")
        @NotBlank(message = OBLIGATORIO) @Email(regexp = EMAIL_REGEX, message = EMAIL_MENSAJE)
        @Size(max = 120, message = "No puede superar 120 caracteres")
        String email
) {
}
