package ar.edu.uade.api.deliverygo.usuarios.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import static ar.edu.uade.api.deliverygo.usuarios.dto.Validaciones.*;

// DTO-01. No incluye id, rol, estado ni fechaAlta: el servidor los asigna y, si llegan, se ignoran (RN-06).
public record RegistrarUsuarioRequest(
        @Schema(example = "Ana")
        @NotBlank(message = OBLIGATORIO) @Size(max = 80, message = "No puede superar 80 caracteres")
        String nombre,

        @Schema(example = "Pérez")
        @NotBlank(message = OBLIGATORIO) @Size(max = 80, message = "No puede superar 80 caracteres")
        String apellido,

        @Schema(example = "ana.perez@example.com", description = "Se normaliza a minúsculas antes de comparar y persistir (DC-02).")
        @NotBlank(message = OBLIGATORIO) @Email(regexp = EMAIL_REGEX, message = EMAIL_MENSAJE)
        @Size(max = 120, message = "No puede superar 120 caracteres")
        String email,

        @Schema(example = "+54 11 5555 0101", nullable = true, description = "Opcional. Ausente, null o en blanco significa sin teléfono.")
        @Size(max = 30, message = "No puede superar 30 caracteres")
        String telefono,

        @Schema(example = "ClaveDemo2026!", accessMode = Schema.AccessMode.WRITE_ONLY, description = "Mínimo 8 caracteres. Nunca se devuelve ni se registra.")
        @NotBlank(message = OBLIGATORIO) @Size(min = 8, message = PASSWORD_MINIMO)
        String password
) {
}
