package ar.edu.uade.api.deliverygo.usuarios.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import static ar.edu.uade.api.deliverygo.usuarios.dto.Validaciones.*;

// DTO-05. El PUT reemplaza todos los datos editables; no admite rol, estado ni contraseña (RN-10).
public record ModificarPerfilRequest(
        @Schema(example = "Ana")
        @NotBlank(message = OBLIGATORIO) @Size(max = 80, message = "No puede superar 80 caracteres")
        String nombre,

        @Schema(example = "Pérez")
        @NotBlank(message = OBLIGATORIO) @Size(max = 80, message = "No puede superar 80 caracteres")
        String apellido,

        @Schema(example = "ana.perez@example.com")
        @NotBlank(message = OBLIGATORIO) @Email(regexp = EMAIL_REGEX, message = EMAIL_MENSAJE)
        @Size(max = 120, message = "No puede superar 120 caracteres")
        String email,

        @Schema(example = "+54 11 5555 0101", nullable = true, description = "Omitir, null o en blanco elimina el teléfono anterior (DC-03).")
        @Size(max = 30, message = "No puede superar 30 caracteres")
        String telefono
) {
}
