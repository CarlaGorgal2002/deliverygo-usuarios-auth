package ar.edu.uade.api.deliverygo.usuarios.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import static ar.edu.uade.api.deliverygo.usuarios.dto.Validaciones.OBLIGATORIO;

// DTO-06. El id lo asigna el servidor y el propietario sale de la ruta; no admite activa ni usuarioId.
public record CrearDireccionRequest(
        @Schema(example = "Av. Independencia")
        @NotBlank(message = OBLIGATORIO) @Size(max = 100, message = "No puede superar 100 caracteres")
        String calle,

        @Schema(example = "1200", description = "Texto: admite numeración alfanumérica o S/N.")
        @NotBlank(message = OBLIGATORIO) @Size(max = 10, message = "No puede superar 10 caracteres")
        String numero,

        @Schema(example = "2", nullable = true)
        @Size(max = 10, message = "No puede superar 10 caracteres")
        String piso,

        @Schema(example = "B", nullable = true)
        @Size(max = 10, message = "No puede superar 10 caracteres")
        String departamento,

        @Schema(example = "Ciudad Autónoma de Buenos Aires")
        @NotBlank(message = OBLIGATORIO) @Size(max = 80, message = "No puede superar 80 caracteres")
        String ciudad,

        @Schema(example = "Ciudad Autónoma de Buenos Aires")
        @NotBlank(message = OBLIGATORIO) @Size(max = 80, message = "No puede superar 80 caracteres")
        String provincia,

        @Schema(example = "C1099AAZ")
        @NotBlank(message = OBLIGATORIO) @Size(max = 10, message = "No puede superar 10 caracteres")
        String codigoPostal,

        @Schema(example = "false", defaultValue = "false",
                description = "Si es true desmarca la anterior en la misma transacción. Si se omite es false, incluso en la primera dirección.")
        Boolean predeterminada
) {
}
